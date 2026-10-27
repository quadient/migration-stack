import type { ModuleMetadata } from "@/types/moduleMetadata.ts";

export type RunStatus = "RUNNING" | "SUCCESS" | "ERROR";

export type Job = {
    id: string;
    moduleId: string;
    status: RunStatus;
    lastUpdated: Date;
    logs: string[] | undefined;
    runId: string;
};

export function getLatestJobForModule(jobs: Job[], module: ModuleMetadata): Job | undefined {
    return jobs
        .filter((job) => job.moduleId === module.id)
        .reduce<Job | undefined>(
            (latest, job) => (!latest || new Date(job.lastUpdated) > new Date(latest.lastUpdated) ? job : latest),
            undefined,
        );
}
