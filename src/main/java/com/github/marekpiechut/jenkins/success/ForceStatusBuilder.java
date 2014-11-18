package com.github.marekpiechut.jenkins.success;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import groovy.util.Eval;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.util.ComboBoxModel;

/**
 * Jenkins plugin to force build result and finish it immediately.
 */
public class ForceStatusBuilder extends Builder {

    private String result;
    private String condition;
    private boolean useCondition;

    @DataBoundConstructor
    public ForceStatusBuilder(String result, String condition, boolean useCondition) {
        this.result = result;
        this.condition = condition;
        this.useCondition = useCondition;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        if(isConditionValid()) {
            Result result = Result.fromString(this.result);
            return tryInterrupt(build, listener, result);
        } else {
            listener.getLogger();
        }
        return true;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return ForceStatusDescriptor.INSTANCE;
    }

    private boolean tryInterrupt(AbstractBuild build, BuildListener listener, Result result) {
        try {
            Executor e = getExecutor(build);
            if (e != null) {
                e.interrupt(result);
                build.setResult(result);
                listener.getLogger().format("Marking build as %s and finishing.\n", result);
            } else {
                listener.getLogger().println("No executor found. Is build running?");
            }
            return true;
        } catch (Exception e) {
            build.setResult(Result.FAILURE);
            listener.getLogger().println("Could not stop build.");
            return false;
        }
    }

    private boolean isConditionValid() {
        if(getUseCondition() && !getCondition().trim().isEmpty()) {
            return (Boolean)Eval.me(getCondition());
        } else {
            return true;
        }
    }

    private Executor getExecutor(AbstractBuild build) {
        Executor e = build.getExecutor();
        if(e == null) {
            e = build.getOneOffExecutor();
        }
        return e;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public boolean getUseCondition() {
        return useCondition;
    }

    public void setUseCondition(boolean useCondition) {
        this.useCondition = useCondition;
    }

    @Extension
    public static class ForceStatusDescriptor extends hudson.model.Descriptor<Builder> {
        static ForceStatusDescriptor INSTANCE = new ForceStatusDescriptor();

        private static final List<String> STATUSES = Arrays.asList(
                Result.SUCCESS.toString(),
                Result.ABORTED.toString(),
                Result.FAILURE.toString(),
                Result.NOT_BUILT.toString(),
                Result.UNSTABLE.toString());
        public static final ComboBoxModel COMBO_MODEL = new ComboBoxModel(STATUSES);

        @Override
        public String getDisplayName() {
            return "Force build result";
        }


        public ComboBoxModel doFillResultItems() {
            return COMBO_MODEL;
        }
    }
}

