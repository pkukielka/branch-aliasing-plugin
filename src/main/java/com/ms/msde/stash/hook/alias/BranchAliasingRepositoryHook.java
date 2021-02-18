package com.ms.msde.stash.hook.alias;

import com.atlassian.bitbucket.hook.repository.PostRepositoryHook;
import com.atlassian.bitbucket.hook.repository.PostRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookRequest;
import com.atlassian.bitbucket.hook.repository.SynchronousPreferred;
import com.atlassian.bitbucket.scm.git.command.GitCommandBuilderFactory;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Collectors;

@ExportAsService
@Component
@SynchronousPreferred
public class BranchAliasingRepositoryHook implements PostRepositoryHook<RepositoryHookRequest> {

    private static final Logger log = LoggerFactory.getLogger(BranchAliasingRepositoryHook.class);
    private final GitCommandBuilderFactory gitCmdBuilderFactory;

    @Autowired
    public BranchAliasingRepositoryHook(@ComponentImport GitCommandBuilderFactory gitCmdBuilderFactory) {
        this.gitCmdBuilderFactory = gitCmdBuilderFactory;
    }

    @Override
    public void postUpdate(@Nonnull PostRepositoryHookContext context,
                           @Nonnull RepositoryHookRequest hookRequest) {
        log.info("[{}] {} updated [{}]",
                hookRequest.getRepository(),
                hookRequest.getTrigger().getId(),
                hookRequest.getRefChanges().stream()
                        .map(change -> change.getRef().getId())
                        .collect(Collectors.joining(", ")));

        Map<String, Object> settings = context.getSettings().asMap();
        hookRequest.getRefChanges().forEach(refChange -> {
            for (int i = 0; i < 5; i++) {
                String branchRef = (String) settings.getOrDefault("branchName" + i, null);
                String aliasRef = (String) settings.getOrDefault("aliasName" + i, null);
                if (branchRef != null && aliasRef != null) {
                    if (refChange.getRef().getId().equals(branchRef)) {
                        log.info("Updating ref {} to {} ({})", aliasRef, refChange.getRef().getId(), refChange.getToHash());
                        gitCmdBuilderFactory.builder(hookRequest.getRepository())
                                .updateRef()
                                .set(aliasRef, refChange.getToHash())
                                .build()
                                .call();
                    }
                }
            }
        });
    }
}