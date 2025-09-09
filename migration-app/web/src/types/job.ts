export type RunStatus = "RUNNING" | "SUCCESS" | "ERROR";

export type Job = {
    id: string;
    moduleId: string;
    status: RunStatus;
    lastUpdated: Date;
    logs: string[] | undefined;
};
