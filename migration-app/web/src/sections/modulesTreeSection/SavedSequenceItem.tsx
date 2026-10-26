import { useState } from "react";
import { ChevronRight, Cog, ListX, Play, Workflow } from "lucide-react";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible.tsx";
import {
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuSeparator,
    ContextMenuTrigger,
} from "@/components/ui/context-menu.tsx";
import { SidebarMenuSub, SidebarMenuSubButton, SidebarMenuSubItem } from "@/components/ui/sidebar.tsx";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip.tsx";
import type { ModuleSelection } from "@/hooks/useModuleSelection.ts";
import { sameSequence } from "@/hooks/useSavedSequences.ts";
import { RunLabel, RunModuleList, RunStatusIcon } from "@/sections/modulesTreeSection/RunSummary.tsx";
import { getName, type ModuleMetadata } from "@/types/moduleMetadata.ts";

type SavedSequenceItemProps = {
    moduleIds: string[];
    modules: ModuleMetadata[];
    selection: ModuleSelection;
    executing: boolean;
    runningModuleIds: Set<string>;
    onExecute: (module: ModuleMetadata) => void;
    onExecuteRun: (moduleIds: string[]) => void;
    onRemove: (moduleIds: string[]) => void;
};

export function SavedSequenceItem({
    moduleIds,
    modules,
    selection,
    executing,
    runningModuleIds,
    onExecute,
    onExecuteRun,
    onRemove,
}: SavedSequenceItemProps) {
    const [open, setOpen] = useState(false);
    const sequenceModules = moduleIds.map((id) => ({ id, module: modules.find((it) => it.id === id) }));
    const existingModuleIds = sequenceModules.flatMap(({ id, module }) => (module ? [id] : []));
    const summaryItems = sequenceModules.map(({ module }) => ({
        name: module ? getName(module) : "Missing module",
    }));

    return (
        <Collapsible asChild open={open} onOpenChange={setOpen} className="group/sequence">
            <SidebarMenuSubItem>
                <ContextMenu>
                    <Tooltip delayDuration={500}>
                        <ContextMenuTrigger asChild>
                            <TooltipTrigger asChild>
                                <SidebarMenuSubButton
                                    asChild
                                    className="peer pl-7"
                                    isActive={sameSequence(selection.moduleIds, existingModuleIds)}
                                    aria-disabled={existingModuleIds.length === 0}
                                >
                                    <button
                                        type="button"
                                        className="w-full"
                                        onClick={() => selection.selectMany(existingModuleIds)}
                                    >
                                        <Workflow />
                                        <RunLabel items={summaryItems} />
                                    </button>
                                </SidebarMenuSubButton>
                            </TooltipTrigger>
                        </ContextMenuTrigger>
                        <TooltipContent side="right">
                            <RunModuleList items={summaryItems} />
                        </TooltipContent>
                    </Tooltip>
                    <ContextMenuContent>
                        <ContextMenuItem
                            disabled={executing || existingModuleIds.length === 0}
                            onSelect={() => onExecuteRun(existingModuleIds)}
                        >
                            <Play />
                            {`Execute ${existingModuleIds.length} Modules`}
                        </ContextMenuItem>
                        <ContextMenuSeparator />
                        <ContextMenuItem onSelect={() => onRemove(moduleIds)}>
                            <ListX />
                            Remove Saved Sequence
                        </ContextMenuItem>
                    </ContextMenuContent>
                </ContextMenu>
                <CollapsibleTrigger className="absolute top-1 left-1 flex size-5 items-center justify-center rounded-sm text-sidebar-foreground hover:bg-sidebar-accent peer-data-[active=true]:text-sidebar-primary-foreground peer-data-[active=true]:hover:bg-sidebar-primary-foreground/20">
                    <ChevronRight className="size-4 transition-transform group-data-[state=open]/sequence:rotate-90" />
                    <span className="sr-only">{open ? "Hide modules" : "Show modules"}</span>
                </CollapsibleTrigger>
                <CollapsibleContent>
                    <SidebarMenuSub>
                        {sequenceModules.map(({ id, module }, index) => (
                            <SidebarMenuSubItem key={index}>
                                <ContextMenu>
                                    <ContextMenuTrigger asChild>
                                        <SidebarMenuSubButton
                                            asChild
                                            isActive={selection.currentModuleId === id}
                                            aria-disabled={!module}
                                        >
                                            <button
                                                type="button"
                                                className="w-full"
                                                onClick={() => module && selection.selectMany(existingModuleIds, id)}
                                            >
                                                {runningModuleIds.has(id) ? (
                                                    <RunStatusIcon status="RUNNING" />
                                                ) : (
                                                    <Cog />
                                                )}
                                                <span className="truncate">{`${index + 1}. ${summaryItems[index].name}`}</span>
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
                        ))}
                    </SidebarMenuSub>
                </CollapsibleContent>
            </SidebarMenuSubItem>
        </Collapsible>
    );
}
