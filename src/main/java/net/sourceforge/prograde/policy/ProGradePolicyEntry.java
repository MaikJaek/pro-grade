/*
 * #%L
 * pro-grade
 * %%
 * Copyright (C) 2013 - 2014 Ondřej Lukáš, Josef Cacek
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.sourceforge.prograde.policy;

import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import net.sourceforge.prograde.debug.ProGradePolicyDebugger;

/**
 * Class representing one policy entry.
 * 
 * @author Ondrej Lukas
 */
public class ProGradePolicyEntry {

    private CodeSource codeSource; // codebase + cert gained from signedby
    private List<ProGradePrincipal> principals;
    private Permissions permissions;
    private boolean neverImplies = false;
    private boolean debug = false;
    // this is only for debug
    private boolean grant;

    /**
     * Constructor of ProgradePolicyEntry.
     * 
     * @param grant true if this policy entry represent grant entry, false if entry represent deny entry
     * @param debug true for writing debug informations, false otherwise
     */
    public ProGradePolicyEntry(boolean grant, boolean debug) {
        principals = new ArrayList<ProGradePrincipal>();
        permissions = new Permissions();
        this.grant = grant;
        this.debug = debug;
    }

    /**
     * Setter of CodeSource of policy entry.
     * 
     * @param codeSource CodeSource of policy entry
     */
    public void setCodeSource(CodeSource codeSource) {
        this.codeSource = codeSource;
    }

    /**
     * Method for adding principal (represent by ProgradePrincipal) to this ProgradePolicyEntry.
     * 
     * @param principal principal for adding
     */
    public void addPrincipal(ProGradePrincipal principal) {
        principals.add(principal);
    }

    /**
     * Method for adding Permission to this ProgradePolicyEntry.
     * 
     * @param permission Permission for adding
     */
    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    /**
     * Method for setting that this ProgradePolicyEntry never implies any Permission.
     * 
     * @param neverImplies true for set that this entry never implies any Permission, false otherwise
     */
    public void setNeverImplies(boolean neverImplies) {
        this.neverImplies = neverImplies;
    }

    /**
     * Method for determining whether this ProgradePolicyEntry implies given permission.
     * 
     * @param pd active ProtectionDomain to test
     * @param permission Permission which need to be determined
     * @return true if ProgradePolicyEntry implies given Permission, false otherwise
     */
    public boolean implies(ProtectionDomain pd, Permission permission) {

        if (neverImplies) {
            if (debug) {
                ProGradePolicyDebugger.log("This entry never imply anything.");
            }
            return false;
        }

        // codesource
        if (codeSource != null && pd.getCodeSource() != null) {
            if (debug) {
                ProGradePolicyDebugger.log("Evaluate codesource...");
                ProGradePolicyDebugger.log("      Policy codesource: " + codeSource.toString());
                ProGradePolicyDebugger.log("      Active codesource: " + pd.getCodeSource().toString());
            }
            if (!codeSource.implies(pd.getCodeSource())) {
                if (debug) {
                    ProGradePolicyDebugger.log("Evaluation (codesource) failed.");
                }
                return false;
            }
        }

        // principals
        if (!principals.isEmpty()) {
            if (debug) {
                ProGradePolicyDebugger.log("Evaluate principals...");
            }
            Principal[] pdPrincipals = pd.getPrincipals();
            if (pdPrincipals == null || pdPrincipals.length == 0) {
                if (debug) {
                    ProGradePolicyDebugger.log("Evaluation (principals) failed. There is no active principals.");
                }
                return false;
            }
            if (debug) {
                ProGradePolicyDebugger.log("Policy principals:");
                for (ProGradePrincipal principal : principals) {
                    ProGradePolicyDebugger.log("      " + principal.toString());
                }
                ProGradePolicyDebugger.log("Active principals:");
                if (pdPrincipals.length == 0) {
                    ProGradePolicyDebugger.log("      none");
                }
                for (int i = 0; i < pdPrincipals.length; i++) {
                    Principal principal = pdPrincipals[i];
                    ProGradePolicyDebugger.log("      " + principal.toString());
                }
            }

            for (ProGradePrincipal principal : principals) {
                boolean contain = false;
                for (int i = 0; i < pdPrincipals.length; i++) {
                    if (principal.hasWildcardClassName()) {
                        contain = true;
                        break;
                    }
                    Principal pdPrincipal = pdPrincipals[i];
                    if (pdPrincipal.getClass().getName().equals(principal.getClassName())) {
                        if (principal.hasWildcardPrincipal()) {
                            contain = true;
                            break;
                        }
                        if (pdPrincipal.getName().equals(principal.getPrincipalName())) {
                            contain = true;
                            break;
                        }
                    }
                }
                if (!contain) {
                    if (debug) {
                        ProGradePolicyDebugger.log("Evaluation (principals) failed.");
                    }
                    return false;
                }
            }
        }

        // permissions
        if (debug) {
            ProGradePolicyDebugger.log("Evaluation codesource/principals passed.");
            String grantOrDeny = (grant) ? "granting" : "denying";
            Enumeration<Permission> elements = permissions.elements();
            while (elements.hasMoreElements()) {
                Permission nextElement = elements.nextElement();
                ProGradePolicyDebugger.log("      " + grantOrDeny + " " + nextElement.toString());
            }
        }

        boolean toReturn = permissions.implies(permission);
        if (debug) {
            if (toReturn) {
                ProGradePolicyDebugger.log("Needed permission found in this entry.");
            } else {
                ProGradePolicyDebugger.log("Needed permission wasn't found in this entry.");
            }
        }
        return toReturn;
    }
}
