package com.atsebak.raspberrypi.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;

public class RaspberryPIRunner extends DefaultProgramRunner {
    private static final String RUNNER_ID = "RaspberryPIRunner";
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState profileState, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        final RunProfile runProfileRaw = environment.getRunProfile();
        if (runProfileRaw instanceof RaspberryPIRunConfiguration) {
            throw  new NotImplementedException("Not Supported yet");
        }
        else {
            return super.doExecute(profileState, environment);
        }
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && (profile instanceof RaspberryPIRunConfiguration);
    }
}
