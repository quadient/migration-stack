import { Button } from "@/components/ui/button.tsx";
import { Card, CardAction, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import { ScrollArea } from "@/components/ui/scroll-area";
import { FileCog, Rocket, Play, LoaderCircle, FileText, CircleCheckBig, CircleX } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { EmptyCard } from "@/common/emptyCard.tsx";
import LogsDialog from "@/dialogs/logs/logsDialog.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import type { SuccessFetchResult } from "@/hooks/useFetch.ts";

export type ModuleMetadata = {
    filename: string;
    category: string;
    path: string;
    displayName: string | undefined;
    sourceFormat: string | undefined;
    description: string | undefined;
};

type RunStatus = "RUNNING" | "SUCCESS" | "ERROR";

export type Job = {
    path: string;
    status: RunStatus;
    lastUpdated: Date;
    logs: string[] | undefined;
};

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
                                        key={module.path}
                                        module={module}
                                        icon={FileCog}
                                        job={jobsResult.data.find((it: Job) => it.path === module.path)}
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
                                        key={module.path}
                                        module={module}
                                        icon={Rocket}
                                        job={jobsResult.data.find((it: Job) => it.path === module.path)}
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

type ModuleCardProps = {
    module: ModuleMetadata;
    icon: LucideIcon;
    job: Job | undefined;
    setJobs: (value: ((prevState: Job[]) => Job[]) | Job[]) => void;
};

function ModuleCard({ module, icon: Icon, job, setJobs }: ModuleCardProps) {
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
                {(!!job) && (
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
                        />
                    </div>
                )}
                <Button className="w-50" type={"submit"} onClick={() => handleExecuteModule(module, setJobs)}>
                    {job?.status === "RUNNING" ? (
                        <>
                            <LoaderCircle className="animate-spin" />
                            Processing...
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

function StatusBadge({ runStatus }: { runStatus: RunStatus }) {
    if (runStatus === "RUNNING") {
        return (
            <Badge className="bg-blue-100 text-blue-800">
                <>
                    <LoaderCircle className="animate-spin" />
                    Running
                </>
            </Badge>
        );
    } else if (runStatus === "SUCCESS") {
        return (
            <Badge className="bg-green-100 text-green-800">
                <>
                    <CircleCheckBig />
                    Success
                </>
            </Badge>
        );
    } else if (runStatus === "ERROR") {
        return (
            <Badge className="bg-red-100 text-red-800">
                <>
                    <CircleX />
                    Error
                </>
            </Badge>
        );
    } else {
        return null;
    }
}

async function handleExecuteModule(
    module: ModuleMetadata,
    setJobs: (value: ((prev: Job[]) => Job[]) | Job[]) => void,
): Promise<void> {
    const path = module.path;

    try {
        const response = await fetch("/api/scripts/runs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ path }),
        });
        setJobs((prev) => {
            const newJob: Job = { path, status: "RUNNING", lastUpdated: new Date(), logs: [] };
            return prev.some((job) => job.path === path)
                ? prev.map((job) => (job.path === path ? newJob : job))
                : [...prev, newJob];
        });

        for await (const line of readLines(response.body!!)) {
            console.log(line);

            if (line.startsWith("result=")) {
                const resultValue = line.substring("result=".length).split(",")[0];
                const status: RunStatus = resultValue === "success" ? "SUCCESS" : "ERROR";

                setJobs((prev) =>
                    prev.map((it) => (it.path === path ? { ...it, status: status, lastUpdated: new Date() } : it)),
                );
            } else {
                setJobs((prev) =>
                    prev.map((it) => (it.path === path ? { ...it, logs: [...(it.logs ?? []), line] } : it)),
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
