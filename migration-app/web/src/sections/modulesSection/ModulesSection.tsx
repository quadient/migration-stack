import { useState } from "react";
import { Button } from "@/components/ui/button.tsx";
import { Card, CardAction, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import { ScrollArea } from "@/components/ui/scroll-area";
import type { LucideIcon } from "lucide-react";
import { FileCog, FileText, LoaderCircle, Play, Rocket } from "lucide-react";
import { EmptyCard } from "@/common/EmptyCard.tsx";
import LogsDialog from "@/dialogs/logs/LogsDialog.tsx";
import type { SuccessFetchResult } from "@/hooks/useFetch.ts";
import type { ModuleMetadata } from "@/types/moduleMetadata.ts";
import type { Job, RunStatus } from "@/types/job.ts";
import { StatusBadge } from "@/common/StatusBadge.tsx";

type ModulesSectionProp = {
    modules: ModuleMetadata[];
    sourceFormat: string | undefined;
    jobsResult: SuccessFetchResult<Job[]>;
};

export default function ModulesSection({ modules, sourceFormat, jobsResult }: ModulesSectionProp) {
    return (
        <ScrollArea className="flex-2">
            <div className="flex gap-6">
                <div className="flex flex-col flex-1 gap-4">
                    <div className="flex items-center h-8 gap-4">
                        <div className="text-lg font-semibold">Parse</div>
                    </div>
                    <div className="flex flex-row gap-4 flex-wrap">
                        {sourceFormat ? (
                            modules
                                .filter(
                                    (module) => module.category === "Parser" && module.sourceFormat === sourceFormat,
                                )
                                .map((module) => (
                                    <ModuleCard
                                        key={module.id}
                                        module={module}
                                        icon={FileCog}
                                        job={getLatestJobForModule(jobsResult, module)}
                                        setJobs={jobsResult.setData}
                                    />
                                ))
                        ) : (
                            <EmptyCard icon={FileCog} message={"Select source format to see available parsers"} />
                        )}
                    </div>
                </div>
                <div className="flex flex-col flex-1 gap-4">
                    <div className="flex items-center h-8 gap-4">
                        <div className="text-lg font-semibold">Deploy</div>
                    </div>
                    <div className="flex flex-row gap-4 flex-wrap">
                        {modules
                            .filter(
                                (module) =>
                                    module.category === "Deployment" &&
                                    module.filename === "DeployDocumentObjects.groovy",
                            )
                            .map((module) => {
                                return (
                                    <ModuleCard
                                        key={module.id}
                                        module={module}
                                        icon={Rocket}
                                        job={getLatestJobForModule(jobsResult, module)}
                                        setJobs={jobsResult.setData}
                                    />
                                );
                            })}
                    </div>
                </div>
            </div>
        </ScrollArea>
    );
}

function getLatestJobForModule(jobsResult: SuccessFetchResult<Job[]>, module: ModuleMetadata) {
    return jobsResult.data
        .sort((a, b) => new Date(b.lastUpdated).getTime() - new Date(a.lastUpdated).getTime())
        .find((it: Job) => it.moduleId === module.id);
}

type ModuleCardProps = {
    module: ModuleMetadata;
    icon: LucideIcon;
    job: Job | undefined;
    setJobs: (value: ((prevState: Job[]) => Job[]) | Job[]) => void;
};

function ModuleCard({ module, icon: Icon, job, setJobs }: ModuleCardProps) {
    const [logDialogOpen, setLogDialogOpen] = useState(false);

    const name = getName(module);

    return (
        <Card className="w-full max-w-sm min-w-75 h-75 flex flex-col" key={module.filename}>
            <CardHeader>
                <CardTitle className="flex items-center gap-2 font-normal max-w-3/5 break-words text-ellipsis leading-snug">
                    <div className="bg-muted rounded-xl p-2.5">
                        <Icon className="w-6 h-6" />
                    </div>
                    {name}
                </CardTitle>
                {!!job && (
                    <CardAction>
                        <StatusBadge runStatus={job.status} />
                    </CardAction>
                )}
            </CardHeader>
            <CardContent className="text-muted-foreground">{module.description}</CardContent>
            <CardFooter className="flex flex-col gap-4 justify-center mt-auto">
                {!!job && (
                    <div className="flex justify-between items-center w-full">
                        <div className="text-muted-foreground text-xs">
                            {`Last run: ${new Date(job.lastUpdated).toLocaleString()}`}
                        </div>
                        <LogsDialog
                            trigger={
                                <Button className="text-muted-foreground text-xs" variant={"ghost"}>
                                    <FileText className="text-muted-foreground" />
                                    View logs
                                </Button>
                            }
                            moduleName={name}
                            job={job}
                            setJobs={setJobs}
                            open={logDialogOpen}
                            setOpen={setLogDialogOpen}
                        />
                    </div>
                )}
                <Button
                    className="w-50"
                    type={"submit"}
                    disabled={job?.status === "RUNNING"}
                    onClick={() => handleExecuteModule(module, setJobs, setLogDialogOpen)}
                >
                    {job?.status === "RUNNING" ? (
                        <>
                            <LoaderCircle className="animate-spin" />
                            Executing...
                        </>
                    ) : (
                        <>
                            <Play className="mr-1" />
                            Execute Module
                        </>
                    )}
                </Button>
            </CardFooter>
        </Card>
    );
}

async function handleExecuteModule(
    module: ModuleMetadata,
    setJobs: (value: ((prev: Job[]) => Job[]) | Job[]) => void,
    setLogDialogOpen: (value: boolean) => void,
): Promise<void> {
    const moduleId = module.id;

    try {
        const response = await fetch("/api/scripts/runs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ id: moduleId }),
        });
        const jobId = response.headers.get("job-id");
        if (!jobId) {
            console.error("No job ID returned from server");
            return;
        }

        setJobs((prev) => {
            const newJob: Job = { id: jobId, moduleId: moduleId, status: "RUNNING", lastUpdated: new Date(), logs: [] };
            return prev.some((job) => job.moduleId === moduleId)
                ? prev.map((job) => (job.moduleId === moduleId ? newJob : job))
                : [...prev, newJob];
        });
        setLogDialogOpen(true);

        for await (const line of readLines(response.body!!)) {
            if (isFinishLine(line, jobId)) {
                const status = getJobStatusFromFinishLine(line);
                setJobs((prev) =>
                    prev.map((it) =>
                        it.moduleId === moduleId ? { ...it, status: status, lastUpdated: new Date() } : it,
                    ),
                );
            } else {
                setJobs((prev) =>
                    prev.map((it) => (it.moduleId === moduleId ? { ...it, logs: [...(it.logs ?? []), line] } : it)),
                );
            }
        }
    } catch (error) {
        console.error("Error executing module:", error);
    }
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

function getName(module: ModuleMetadata): string {
    return module.displayName || module.filename.replace(".groovy", "");
}

function isFinishLine(line: string, jobId: string): boolean {
    return line.startsWith(`id=${jobId};`);
}

function getJobStatusFromFinishLine(line: string): RunStatus {
    const finishLineParts = line.split(";");
    const resultKey = "result";
    const resultPart = finishLineParts.find((it) => it.startsWith(resultKey));
    const resultValue = resultPart?.substring(resultKey.length + 1);
    return resultValue === "success" ? "SUCCESS" : "ERROR";
}
