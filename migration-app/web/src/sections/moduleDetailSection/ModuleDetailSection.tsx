import { FileText, Star } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip.tsx";
import { cn } from "@/lib/utils.ts";
import { EmptyCard } from "@/common/EmptyCard.tsx";
import { ExecuteModuleButton } from "@/common/ExecuteModuleButton.tsx";
import { JobLogs } from "@/common/JobLogs.tsx";
import { StatusBadge } from "@/common/StatusBadge.tsx";
import { getName, type ModuleMetadata } from "@/types/moduleMetadata.ts";
import type { Job } from "@/types/job.ts";

type ModuleDetailSectionProps = {
    module: ModuleMetadata;
    job: Job | undefined;
    latestJob: Job | undefined;
    setJobs: (value: ((prev: Job[]) => Job[]) | Job[]) => void;
    favorite: boolean;
    onShowLatestRun: (module: ModuleMetadata) => void;
    onToggleFavorite: (module: ModuleMetadata) => void;
};

export default function ModuleDetailSection({
    module,
    job,
    latestJob,
    setJobs,
    favorite,
    onShowLatestRun,
    onToggleFavorite,
}: ModuleDetailSectionProps) {
    const favoriteLabel = favorite ? "Remove from favorites" : "Add to favorites";
    const showingEarlierRun = !!job && job.id !== latestJob?.id;

    return (
        <div className="flex flex-col flex-1 min-h-0 gap-4 p-6">
            <div className="flex justify-between items-start gap-4">
                <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2">
                        <div className="font-heading text-xl font-semibold">{getName(module)}</div>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    className="size-8"
                                    onClick={() => onToggleFavorite(module)}
                                >
                                    <Star className={cn(favorite && "fill-q-orange text-q-orange")} />
                                    <span className="sr-only">{favoriteLabel}</span>
                                </Button>
                            </TooltipTrigger>
                            <TooltipContent>{favoriteLabel}</TooltipContent>
                        </Tooltip>
                        {!!job && <StatusBadge runStatus={job.status} />}
                    </div>
                    <div className="text-muted-foreground">{module.description}</div>
                    {!!job && (
                        <div className="flex items-center gap-2 text-muted-foreground text-xs">
                            {showingEarlierRun
                                ? `Earlier run: ${new Date(job.lastUpdated).toLocaleString()}`
                                : `Last run: ${new Date(job.lastUpdated).toLocaleString()}`}
                            {showingEarlierRun && (
                                <Button
                                    variant="link"
                                    className="h-auto p-0 text-xs"
                                    onClick={() => onShowLatestRun(module)}
                                >
                                    Show latest run
                                </Button>
                            )}
                        </div>
                    )}
                </div>
                <ExecuteModuleButton
                    module={module}
                    job={latestJob}
                    setJobs={setJobs}
                    onStarted={() => onShowLatestRun(module)}
                />
            </div>
            {job ? (
                <div className="flex-1 min-h-0">
                    <JobLogs job={job} setJobs={setJobs} />
                </div>
            ) : (
                <EmptyCard icon={FileText} message="Execute the module to see its logs" />
            )}
        </div>
    );
}
