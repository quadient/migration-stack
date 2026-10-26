import { useEffect, useState } from "react";
import type { ModuleMetadata } from "@/types/moduleMetadata.ts";
import type { Job } from "@/types/job.ts";

const MODULE_URL_PARAM = "module";
const JOB_URL_PARAM = "job";

export type ModuleSelection = {
    moduleIds: string[];
    currentModuleId: string | undefined;
    jobId: string | undefined;
    select: (module: ModuleMetadata) => void;
    toggle: (module: ModuleMetadata) => void;
    selectMany: (moduleIds: string[], shownModuleId?: string) => void;
    selectJob: (job: Job) => void;
    followJob: (job: Job, previousJobId: string | undefined) => void;
};

type SelectionState = {
    moduleIds: string[];
    shownModuleId: string | undefined;
    jobId: string | undefined;
};

export function useModuleSelection(): ModuleSelection {
    const [state, setState] = useState(loadSelection);
    const { moduleIds, jobId } = state;

    useEffect(() => saveSelection(state), [state]);

    const selectMany = (moduleIds: string[], shownModuleId?: string) =>
        setState({ moduleIds, shownModuleId, jobId: undefined });

    const toggle = (module: ModuleMetadata) =>
        selectMany(
            moduleIds.includes(module.id) ? moduleIds.filter((id) => id !== module.id) : [...moduleIds, module.id],
        );

    const followJob = (job: Job, previousJobId: string | undefined) =>
        setState((prev) =>
            prev.jobId === previousJobId ? { ...prev, shownModuleId: job.moduleId, jobId: job.id } : prev,
        );

    return {
        moduleIds,
        currentModuleId: getCurrentModuleId(state),
        jobId,
        select: (module) => selectMany([module.id]),
        toggle,
        selectMany,
        selectJob: (job) => setState({ moduleIds: [job.moduleId], shownModuleId: undefined, jobId: job.id }),
        followJob,
    };
}

function getCurrentModuleId({ moduleIds, shownModuleId }: SelectionState): string | undefined {
    return shownModuleId ?? moduleIds.at(-1);
}

function loadSelection(): SelectionState {
    const params = new URLSearchParams(window.location.search);
    const moduleId = params.get(MODULE_URL_PARAM);
    return {
        moduleIds: moduleId ? [moduleId] : [],
        shownModuleId: undefined,
        jobId: (moduleId && params.get(JOB_URL_PARAM)) || undefined,
    };
}

function saveSelection(state: SelectionState) {
    const url = new URL(window.location.href);
    setOrDelete(url.searchParams, MODULE_URL_PARAM, getCurrentModuleId(state));
    setOrDelete(url.searchParams, JOB_URL_PARAM, state.jobId);
    window.history.replaceState(null, "", url);
}

function setOrDelete(params: URLSearchParams, name: string, value: string | undefined) {
    if (value) {
        params.set(name, value);
    } else {
        params.delete(name);
    }
}
