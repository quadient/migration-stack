import { useState } from "react";
import { ChevronRight, ListPlus, ListX, Play } from "lucide-react";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible.tsx";
import {
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuSeparator,
    ContextMenuTrigger,
} from "@/components/ui/context-menu.tsx";
import {
    SidebarGroup,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarMenuSub,
    SidebarMenuSubButton,
    SidebarMenuSubItem,
} from "@/components/ui/sidebar.tsx";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip.tsx";
import type { SavedSequences } from "@/hooks/useSavedSequences.ts";
import { RunLabel, RunModuleList, RunStatusIcon } from "@/sections/modulesTreeSection/RunSummary.tsx";
import { getName, type ModuleMetadata } from "@/types/moduleMetadata.ts";
import type { Job, RunStatus } from "@/types/job.ts";

const COLLAPSED_HISTORY_SIZE = 5;

type Run = { key: string; jobs: Job[]; lastUpdated: Date };

type RunHistoryGroupProps = {
    jobs: Job[];
    modules: ModuleMetadata[];
    shownJobId: string | undefined;
    savedSequences: SavedSequences;
    onSelectJob: (job: Job) => void;
    onExecute: (module: ModuleMetadata) => void;
    onExecuteRun: (moduleIds: string[]) => void;
};

export function RunHistoryGroup({
    jobs,
    modules,
    shownJobId,
    savedSequences,
    onSelectJob,
    onExecute,
    onExecuteRun,
}: RunHistoryGroupProps) {
    const [open, setOpen] = useState(false);
    const [expanded, setExpanded] = useState(false);
    const runs = groupRuns(jobs);
    const visibleRuns = expanded ? runs : runs.slice(0, COLLAPSED_HISTORY_SIZE);

    return (
        <Collapsible open={open} onOpenChange={setOpen} className="group/run-history">
            <SidebarGroup className="group-data-[collapsible=icon]:hidden">
                <SidebarGroupLabel
                    asChild
                    className="w-full gap-2 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                >
                    <CollapsibleTrigger>
                        <ChevronRight className="transition-transform group-data-[state=open]/run-history:rotate-90" />
                        Run history
                    </CollapsibleTrigger>
                </SidebarGroupLabel>
                <CollapsibleContent>
                    <SidebarGroupContent>
                        <SidebarMenu>
                            {runs.length === 0 && (
                                <SidebarMenuItem className="px-2 text-sm text-muted-foreground">
                                    Executed modules will appear here
                                </SidebarMenuItem>
                            )}
                            {visibleRuns.map((run) =>
                                run.jobs.length === 1 ? (
                                    <JobItem
                                        key={run.key}
                                        job={run.jobs[0]}
                                        module={findModule(modules, run.jobs[0])}
                                        shown={run.jobs[0].id === shownJobId}
                                        onSelectJob={onSelectJob}
                                        onExecute={onExecute}
                                    />
                                ) : (
                                    <RunItem
                                        key={run.key}
                                        run={run}
                                        modules={modules}
                                        shownJobId={shownJobId}
                                        savedSequences={savedSequences}
                                        onSelectJob={onSelectJob}
                                        onExecute={onExecute}
                                        onExecuteRun={onExecuteRun}
                                    />
                                ),
                            )}
                            {runs.length > COLLAPSED_HISTORY_SIZE && (
                                <SidebarMenuItem>
                                    <SidebarMenuButton
                                        className="text-xs text-muted-foreground"
                                        onClick={() => setExpanded(!expanded)}
                                    >
                                        {expanded ? "Show less" : `Show all ${runs.length} runs`}
                                    </SidebarMenuButton>
                                </SidebarMenuItem>
                            )}
                        </SidebarMenu>
                    </SidebarGroupContent>
                </CollapsibleContent>
            </SidebarGroup>
        </Collapsible>
    );
}

type JobItemProps = {
    job: Job;
    module: ModuleMetadata | undefined;
    shown: boolean;
    onSelectJob: (job: Job) => void;
    onExecute: (module: ModuleMetadata) => void;
};

function JobItem({ job, module, shown, onSelectJob, onExecute }: JobItemProps) {
    const lastUpdated = new Date(job.lastUpdated);
    return (
        <ContextMenu>
            <Tooltip delayDuration={500}>
                <ContextMenuTrigger asChild>
                    <TooltipTrigger asChild>
                        <SidebarMenuItem>
                            <SidebarMenuButton isActive={shown} disabled={!module} onClick={() => onSelectJob(job)}>
                                <RunStatusIcon status={job.status} />
                                <span className="truncate">{module ? getName(module) : "Missing module"}</span>
                                <RunTime date={lastUpdated} />
                            </SidebarMenuButton>
                        </SidebarMenuItem>
                    </TooltipTrigger>
                </ContextMenuTrigger>
                <TooltipContent side="right">{lastUpdated.toLocaleString()}</TooltipContent>
            </Tooltip>
            <ContextMenuContent>
                <ContextMenuItem disabled={!module} onSelect={() => module && onExecute(module)}>
                    <Play />
                    Execute Module
                </ContextMenuItem>
            </ContextMenuContent>
        </ContextMenu>
    );
}

type RunItemProps = {
    run: Run;
    modules: ModuleMetadata[];
    shownJobId: string | undefined;
    savedSequences: SavedSequences;
    onSelectJob: (job: Job) => void;
    onExecute: (module: ModuleMetadata) => void;
    onExecuteRun: (moduleIds: string[]) => void;
};

function RunItem({ run, modules, shownJobId, savedSequences, onSelectJob, onExecute, onExecuteRun }: RunItemProps) {
    const [open, setOpen] = useState(() => run.jobs.some((job) => job.id === shownJobId));
    const runModules = run.jobs.flatMap((job) => findModule(modules, job) ?? []);
    const runModuleIds = run.jobs.map((job) => job.moduleId);
    const savedSequence = savedSequences.isSaved(runModuleIds);
    const getJobName = (job: Job) => {
        const module = findModule(modules, job);
        return module ? getName(module) : "Missing module";
    };
    const summaryItems = run.jobs.map((job) => ({ name: getJobName(job), status: job.status }));

    return (
        <Collapsible open={open} onOpenChange={setOpen} className="group/collapsible">
            <SidebarMenuItem>
                <ContextMenu>
                    <Tooltip delayDuration={500}>
                        <ContextMenuTrigger asChild>
                            <TooltipTrigger asChild>
                                <CollapsibleTrigger asChild>
                                    <SidebarMenuButton>
                                        <ChevronRight className="transition-transform group-data-[state=open]/collapsible:rotate-90" />
                                        <RunStatusIcon status={getRunStatus(run)} />
                                        <RunLabel items={summaryItems} />
                                        <RunTime date={run.lastUpdated} />
                                    </SidebarMenuButton>
                                </CollapsibleTrigger>
                            </TooltipTrigger>
                        </ContextMenuTrigger>
                        <TooltipContent side="right">
                            <div className="mb-1 opacity-70">{run.lastUpdated.toLocaleString()}</div>
                            <RunModuleList items={summaryItems} />
                        </TooltipContent>
                    </Tooltip>
                    <ContextMenuContent>
                        <ContextMenuItem
                            disabled={runModules.length === 0}
                            onSelect={() => onExecuteRun(runModules.map((module) => module.id))}
                        >
                            <Play />
                            {`Execute ${runModules.length} Modules Again`}
                        </ContextMenuItem>
                        <ContextMenuSeparator />
                        <ContextMenuItem onSelect={() => savedSequences.toggle(runModuleIds)}>
                            {savedSequence ? <ListX /> : <ListPlus />}
                            {savedSequence ? "Remove from Saved Sequences" : "Save Run as Sequence"}
                        </ContextMenuItem>
                    </ContextMenuContent>
                </ContextMenu>
                <CollapsibleContent>
                    <SidebarMenuSub>
                        {run.jobs.map((job) => {
                            const module = findModule(modules, job);
                            return (
                                <SidebarMenuSubItem key={job.id}>
                                    <ContextMenu>
                                        <ContextMenuTrigger asChild>
                                            <SidebarMenuSubButton
                                                asChild
                                                isActive={job.id === shownJobId}
                                                aria-disabled={!module}
                                            >
                                                <button
                                                    type="button"
                                                    className="w-full"
                                                    onClick={() => onSelectJob(job)}
                                                >
                                                    <RunStatusIcon status={job.status} />
                                                    <span className="truncate">{getJobName(job)}</span>
                                                </button>
                                            </SidebarMenuSubButton>
                                        </ContextMenuTrigger>
                                        <ContextMenuContent>
                                            <ContextMenuItem
                                                disabled={!module}
                                                onSelect={() => module && onExecute(module)}
                                            >
                                                <Play />
                                                Execute Module
                                            </ContextMenuItem>
                                        </ContextMenuContent>
                                    </ContextMenu>
                                </SidebarMenuSubItem>
                            );
                        })}
                    </SidebarMenuSub>
                </CollapsibleContent>
            </SidebarMenuItem>
        </Collapsible>
    );
}

function RunTime({ date }: { date: Date }) {
    const today = new Date().toDateString() === date.toDateString();
    return (
        <span className="ml-auto shrink-0 text-xs tabular-nums opacity-70">
            {today
                ? date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
                : date.toLocaleDateString([], { day: "numeric", month: "numeric" })}
        </span>
    );
}

function groupRuns(jobs: Job[]): Run[] {
    const runs = new Map<string, Job[]>();
    for (const job of jobs) {
        runs.set(job.runId, [...(runs.get(job.runId) ?? []), job]);
    }

    return [...runs.entries()]
        .map(([key, runJobs]): Run => {
            const sortedJobs = runJobs.sort((a, b) => getTime(a) - getTime(b));
            return { key, jobs: sortedJobs, lastUpdated: new Date(sortedJobs[sortedJobs.length - 1].lastUpdated) };
        })
        .sort((a, b) => b.lastUpdated.getTime() - a.lastUpdated.getTime());
}

function getRunStatus(run: Run): RunStatus {
    if (run.jobs.some((job) => job.status === "RUNNING")) {
        return "RUNNING";
    }
    return run.jobs.some((job) => job.status === "ERROR") ? "ERROR" : "SUCCESS";
}

function getTime(job: Job): number {
    return new Date(job.lastUpdated).getTime();
}

function findModule(modules: ModuleMetadata[], job: Job): ModuleMetadata | undefined {
    return modules.find((module) => module.id === job.moduleId);
}
