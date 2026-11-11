import { useState } from "react";
import { Button } from "@/components/ui/button.tsx";
import { Card, CardAction, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import { ScrollArea } from "@/components/ui/scroll-area";
import type { LucideIcon } from "lucide-react";
import { FileCog, FileText, Rocket } from "lucide-react";
import { EmptyCard } from "@/common/EmptyCard.tsx";
import { ExecuteModuleButton } from "@/common/ExecuteModuleButton.tsx";
import { StatusBadge } from "@/common/StatusBadge.tsx";
import LogsDialog from "@/dialogs/logs/LogsDialog.tsx";
import type { SuccessFetchResult } from "@/hooks/useFetch.ts";
import { getName, type ModuleMetadata } from "@/types/moduleMetadata.ts";
import { getLatestJobForModule, type Job } from "@/types/job.ts";

type ModulesSectionProp = {
    modules: ModuleMetadata[];
    sourceFormat: string | undefined;
    jobsResult: SuccessFetchResult<Job[]>;
};

export default function ModulesSection({ modules, sourceFormat, jobsResult }: ModulesSectionProp) {
    const parsers = modules
        .filter((module) => module.category === "Parser" && module.sourceFormat === sourceFormat)
        .sort(byOrder);
    const deployments = modules
        .filter((module) => module.category === "Deployment" && module.filename === "DeployDocumentObjects.groovy")
        .sort(byOrder);

    const moduleCard = (module: ModuleMetadata, icon: LucideIcon) => (
        <ModuleCard
            key={module.id}
            module={module}
            icon={icon}
            job={getLatestJobForModule(jobsResult.data, module)}
            setJobs={jobsResult.setData}
        />
    );

    return (
        <ScrollArea className="flex-2">
            <div className="flex gap-6">
                <div className="flex flex-col flex-1 gap-4">
                    <div className="flex items-center h-8 gap-4">
                        <div className="text-lg font-semibold">Parse</div>
                    </div>
                    <div className="flex flex-row gap-4 flex-wrap">
                        {sourceFormat ? (
                            parsers.map((module) => moduleCard(module, FileCog))
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
                        {deployments.map((module) => moduleCard(module, Rocket))}
                    </div>
                </div>
            </div>
        </ScrollArea>
    );
}

function byOrder(a: ModuleMetadata, b: ModuleMetadata): number {
    return (a.order ?? Number.MAX_SAFE_INTEGER) - (b.order ?? Number.MAX_SAFE_INTEGER);
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
        <Card className="w-full max-w-sm min-w-75 min-h-75 flex flex-col">
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
                <ExecuteModuleButton
                    className="w-50"
                    module={module}
                    job={job}
                    setJobs={setJobs}
                    onStarted={() => setLogDialogOpen(true)}
                />
            </CardFooter>
        </Card>
    );
}
