package com.ms.msde.stash.hook.alias;

import com.atlassian.bitbucket.hook.repository.PostRepositoryHook;
import com.atlassian.bitbucket.hook.repository.PostRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookRequest;
import com.atlassian.bitbucket.hook.repository.SynchronousPreferred;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.git.command.GitCommandBuilderFactory;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

@ExportAsService
@Component
@SynchronousPreferred
public class BranchAliasingRepositoryHook implements PostRepositoryHook<RepositoryHookRequest> {

    private static final Logger log = LoggerFactory.getLogger(BranchAliasingRepositoryHook.class);
    private final GitCommandBuilderFactory gitCmdBuilderFactory;

    // Branch ref and alias must have common merge base and alias must be ancestor of the branch (as it will be always lagging behind it)
    private boolean isAncestor(Repository repository, String branchRef, String aliasRef) {
        MergeCommandExitHandler exitHandler = new MergeCommandExitHandler();
        StringCommandOutputHandler stringCommandOutputHandler = new StringCommandOutputHandler();
        gitCmdBuilderFactory.builder(repository)
                .command("merge-base")
                .argument("--is-ancestor")
                .argument(aliasRef)
                .argument(branchRef)
                .exitHandler(exitHandler)
                .build(stringCommandOutputHandler)
                .call();
        return exitHandler.wasSuccessful;
    }

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

        hookRequest.getRefChanges().forEach(refChange -> {
            for (int i = 0; i < 5; i++) {
                String branchRef = context.getSettings().getString("branchName" + i, "");
                String aliasRef = context.getSettings().getString("aliasName" + i, "");

                if (!branchRef.isEmpty() && !aliasRef.isEmpty() && refChange.getRef().getId().equals(branchRef)) {
                    if (isAncestor(hookRequest.getRepository(), branchRef, aliasRef)) {
                        log.info("Updating ref {} to {} ({})", aliasRef, refChange.getRef().getId(), refChange.getToHash());
                        gitCmdBuilderFactory.builder(hookRequest.getRepository())
                                .updateRef()
                                .set(aliasRef, refChange.getToHash())
                                .build()
                                .call();

                    } else {
                        log.error("Failure to update an alias - {} is not ancestor of {} ({})!",
                                aliasRef, refChange.getRef().getId(), refChange.getToHash());
                    }
                }
            }
        });
    }
}