package com.dysi.AngryBoxClientManager.config;

import com.dysi.AngryBoxClientManager.models.AuthorityName;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Una única fuente contable para el padrón global de ClientManager. */
@Component
@Validated
@ConfigurationProperties(prefix = "angrybox.sync")
public class AngryboxSyncProperties {
    public enum CodeTarget { EXTERNAL_CODE, ID }
    public enum MatchingMode { CODE, DOCUMENT, CODE_AND_DOCUMENT, CODE_OR_DOCUMENT }

    private boolean enabled;
    private boolean syncDeactivations;
    private Long sourceCompanyId;
    @NotNull
    private AuthorityName requiredAuthority = AuthorityName.ROLE_API;

    @NotNull
    private CodeTarget codeTarget = CodeTarget.EXTERNAL_CODE;
    @NotNull
    private MatchingMode matchingMode = MatchingMode.CODE_AND_DOCUMENT;
    @Min(0)
    @Max(3600)
    private int maxFutureSeconds = 300;

    @AssertTrue(message = "Configurá angrybox.sync.source-company-id positivo antes de habilitar la recepción.")
    public boolean isSourceConfigured() {
        return !enabled || sourceCompanyId != null && sourceCompanyId > 0;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSyncDeactivations() {
        return syncDeactivations;
    }

    public void setSyncDeactivations(boolean syncDeactivations) {
        this.syncDeactivations = syncDeactivations;
    }

    public Long getSourceCompanyId() {
        return sourceCompanyId;
    }

    public void setSourceCompanyId(Long sourceCompanyId) {
        this.sourceCompanyId = sourceCompanyId;
    }

    public AuthorityName getRequiredAuthority() {
        return requiredAuthority;
    }

    public void setRequiredAuthority(AuthorityName requiredAuthority) {
        this.requiredAuthority = requiredAuthority;
    }

    public CodeTarget getCodeTarget() {
        return codeTarget;
    }

    public void setCodeTarget(CodeTarget codeTarget) {
        this.codeTarget = codeTarget;
    }

    public MatchingMode getMatchingMode() {
        return matchingMode;
    }

    public void setMatchingMode(MatchingMode matchingMode) {
        this.matchingMode = matchingMode;
    }

    public int getMaxFutureSeconds() {
        return maxFutureSeconds;
    }

    public void setMaxFutureSeconds(int maxFutureSeconds) {
        this.maxFutureSeconds = maxFutureSeconds;
    }
}
