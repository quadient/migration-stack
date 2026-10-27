import type { Job, RunStatus } from "@/types/job.ts";

type SetJobs = (value: ((prev: Job[]) => Job[]) | Job[]) => void;

export type ExecuteModulesCallbacks = {
    onJobStarted?: (index: number, job: Job) => void;
    onJobFinished?: (index: number, status: RunStatus) => void;
};

type Marker = {
    jobId: string;
    moduleId: string | undefined;
    runId: string | undefined;
    result: "running" | "success" | "error";
};

const MARKER_PATTERN =
    /^id=([0-9a-f-]{36});(?:moduleId=([^;]+);)?(?:runId=([0-9a-f-]{36});)?result=(running|success|error)/;

export async function executeModules(
    moduleIds: string[],
    setJobs: SetJobs,
    { onJobStarted, onJobFinished }: ExecuteModulesCallbacks = {},
): Promise<string | undefined> {
    try {
        const response = await fetch("/api/scripts/runs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ ids: moduleIds }),
        });
        if (!response.ok || !response.body) {
            const error = await response.text();
            console.error("Failed to execute modules:", error);
            return error;
        }

        let index = -1;
        let currentJobId: string | undefined;

        for await (const line of readLines(response.body)) {
            const marker = parseMarker(line);
            if (marker?.result === "running" && marker.moduleId && marker.runId) {
                index += 1;
                const { jobId, moduleId, runId } = marker;
                currentJobId = jobId;

                const newJob: Job = {
                    id: jobId,
                    moduleId,
                    status: "RUNNING",
                    lastUpdated: new Date(),
                    logs: [],
                    runId,
                };
                setJobs((prev) => [...prev, newJob]);
                onJobStarted?.(index, newJob);
            } else if (currentJobId && marker?.jobId === currentJobId) {
                const jobId = currentJobId;
                const status: RunStatus = marker.result === "success" ? "SUCCESS" : "ERROR";
                setJobs((prev) =>
                    prev.map((it) => (it.id === jobId ? { ...it, status, lastUpdated: new Date() } : it)),
                );
                onJobFinished?.(index, status);
            } else if (currentJobId) {
                const jobId = currentJobId;
                setJobs((prev) =>
                    prev.map((it) => (it.id === jobId ? { ...it, logs: [...(it.logs ?? []), line] } : it)),
                );
            }
        }
    } catch (error) {
        console.error("Error executing modules:", error);
        return String(error);
    }
}

function parseMarker(line: string): Marker | undefined {
    const match = MARKER_PATTERN.exec(line);
    if (!match) {
        return undefined;
    }
    return { jobId: match[1], moduleId: match[2], runId: match[3], result: match[4] as Marker["result"] };
}

async function* readLines(stream: ReadableStream<Uint8Array>) {
    const reader = stream.getReader();
    let last = "";
    try {
        while (true) {
            const { value, done } = await reader.read();
            if (done) {
                if (last !== "") {
                    yield last;
                }
                break;
            }
            last += new TextDecoder().decode(value);
            const parts = last.split("\n");

            for (let i = 0; i < parts.length - 1; i += 1) {
                yield parts[i];
            }

            last = parts[parts.length - 1];
        }
    } finally {
        reader.releaseLock();
    }
}
