import { type MouseEvent, type ReactNode, useState } from "react";
import {
    ChevronRight,
    Cog,
    FolderCog,
    ListOrdered,
    ListPlus,
    ListX,
    LoaderCircle,
    type LucideIcon,
    Play,
    RefreshCw,
    Search,
    Star,
    StarOff,
} from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import {
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuSeparator,
    ContextMenuTrigger,
} from "@/components/ui/context-menu.tsx";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible.tsx";
import {
    Sidebar,
    SidebarContent,
    SidebarGroup,
    SidebarGroupAction,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarHeader,
    SidebarInput,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarMenuSkeleton,
    SidebarMenuSub,
    SidebarMenuSubButton,
    SidebarMenuSubItem,
    SidebarTrigger,
    useSidebar,
} from "@/components/ui/sidebar.tsx";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip.tsx";
import type { UseFetchResult } from "@/hooks/useFetch.ts";
import type { Favorites } from "@/hooks/useFavorites.ts";
import type { ModuleSelection } from "@/hooks/useModuleSelection.ts";
import type { SavedSequences } from "@/hooks/useSavedSequences.ts";
import { cn } from "@/lib/utils.ts";
import { getName, type ModuleMetadata } from "@/types/moduleMetadata.ts";
import { RunHistoryGroup } from "@/sections/modulesTreeSection/RunHistoryGroup.tsx";
import { RunStatusIcon } from "@/sections/modulesTreeSection/RunSummary.tsx";
import { SavedSequenceItem } from "@/sections/modulesTreeSection/SavedSequenceItem.tsx";
import type { Job } from "@/types/job.ts";

type ModulesTreeSectionProps = {
    modulesResult: UseFetchResult<ModuleMetadata[]>;
    jobsResult: UseFetchResult<Job[]>;
    sourceFormat: string | undefined;
    shownJobId: string | undefined;
    selection: ModuleSelection;
    favorites: Favorites;
    savedSequences: SavedSequences;
    executingSelection: boolean;
    onExecute: (module: ModuleMetadata) => void;
    onExecuteSelection: () => void;
    onExecuteRun: (moduleIds: string[]) => void;
};

export default function ModulesTreeSection({
    modulesResult,
    jobsResult,
    sourceFormat,
    shownJobId,
    selection,
    favorites,
    savedSequences,
    executingSelection,
    onExecute,
    onExecuteSelection,
    onExecuteRun,
}: ModulesTreeSectionProps) {
    const [filter, setFilter] = useState("");
    const query = filter.trim().toLowerCase();
    const filteredModules =
        modulesResult.status === "ok"
            ? modulesResult.data.filter(
                  (module) => matchesSourceFormat(module, sourceFormat) && matchesQuery(module, query),
              )
            : [];
    const groups = groupModules(filteredModules);
    const favoriteModules = favorites.moduleIds.flatMap((id) => filteredModules.filter((module) => module.id === id));
    const runningModuleIds = new Set(
        jobsResult.status === "ok"
            ? jobsResult.data.filter((job) => job.status === "RUNNING").map((job) => job.moduleId)
            : [],
    );
    const visibleSequences = savedSequences.sequences.filter((sequence) =>
        sequence.some((id) => filteredModules.some((module) => module.id === id)),
    );

    const [reloading, setReloading] = useState(false);
    const [reloadError, setReloadError] = useState<string>();

    const reloadModules = async () => {
        setReloading(true);
        setReloadError(undefined);
        try {
            const response = await fetch("/api/scripts/reload", { method: "POST" });
            if (response.ok) {
                modulesResult.refetch();
            } else {
                setReloadError(await response.text());
            }
        } catch (err) {
            setReloadError(String(err));
        } finally {
            setReloading(false);
        }
    };

    return (
        <Sidebar collapsible="icon">
            <SidebarHeader className="h-12 flex-row items-center justify-end border-b border-sidebar-border">
                <div className="relative flex-1 group-data-[collapsible=icon]:hidden">
                    <Search className="pointer-events-none absolute top-1/2 left-2 size-4 -translate-y-1/2 opacity-50" />
                    <SidebarInput
                        placeholder="Filter modules"
                        value={filter}
                        onChange={(event) => setFilter(event.target.value)}
                        className="pl-8"
                    />
                </div>
                <SidebarTrigger />
            </SidebarHeader>
            <SidebarContent className="select-none">
                {modulesResult.status === "ok" && jobsResult.status === "ok" && (
                    <RunHistoryGroup
                        jobs={jobsResult.data}
                        modules={modulesResult.data}
                        shownJobId={shownJobId}
                        savedSequences={savedSequences}
                        onSelectJob={selection.selectJob}
                        onExecute={onExecute}
                        onExecuteRun={onExecuteRun}
                    />
                )}
                <SidebarGroup>
                    <SidebarGroupLabel className="gap-2 pr-7">
                        Modules
                        {selection.moduleIds.length > 1 && (
                            <SelectionActions
                                count={selection.moduleIds.length}
                                executing={executingSelection}
                                onExecute={onExecuteSelection}
                            />
                        )}
                    </SidebarGroupLabel>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <SidebarGroupAction disabled={reloading} onClick={reloadModules}>
                                <RefreshCw className={cn(reloading && "animate-spin")} />
                                <span className="sr-only">Reload modules</span>
                            </SidebarGroupAction>
                        </TooltipTrigger>
                        <TooltipContent side="right">Reload modules</TooltipContent>
                    </Tooltip>
                    <SidebarGroupContent>
                        <SidebarMenu>
                            {reloadError && (
                                <SidebarMenuItem className="px-2 text-sm text-destructive group-data-[collapsible=icon]:hidden">
                                    {reloadError}
                                </SidebarMenuItem>
                            )}
                            {modulesResult.status === "loading" &&
                                Array.from({ length: 5 }, (_, index) => (
                                    <SidebarMenuItem key={index}>
                                        <SidebarMenuSkeleton showIcon />
                                    </SidebarMenuItem>
                                ))}
                            {modulesResult.status === "error" && (
                                <SidebarMenuItem className="px-2 text-sm text-destructive">
                                    Failed to load modules
                                </SidebarMenuItem>
                            )}
                            {modulesResult.status === "ok" && groups.length === 0 && (
                                <SidebarMenuItem className="px-2 text-sm text-muted-foreground group-data-[collapsible=icon]:hidden">
                                    No matching modules
                                </SidebarMenuItem>
                            )}
                            {favoriteModules.length > 0 && (
                                <ModuleTreeGroup
                                    name="favorites"
                                    icon={Star}
                                    hideFavoriteIcons
                                    modules={favoriteModules}
                                    forceOpen={query !== ""}
                                    selection={selection}
                                    favorites={favorites}
                                    savedSequences={savedSequences}
                                    executingSelection={executingSelection}
                                    runningModuleIds={runningModuleIds}
                                    onExecute={onExecute}
                                    onExecuteSelection={onExecuteSelection}
                                />
                            )}
                            {modulesResult.status === "ok" && visibleSequences.length > 0 && (
                                <ModuleTreeGroup
                                    name="saved sequences"
                                    icon={ListOrdered}
                                    modules={[]}
                                    forceOpen={query !== ""}
                                    selection={selection}
                                    favorites={favorites}
                                    savedSequences={savedSequences}
                                    executingSelection={executingSelection}
                                    runningModuleIds={runningModuleIds}
                                    onExecute={onExecute}
                                    onExecuteSelection={onExecuteSelection}
                                >
                                    {visibleSequences.map((sequence) => (
                                        <SavedSequenceItem
                                            key={sequence.join("\n")}
                                            moduleIds={sequence}
                                            modules={modulesResult.data}
                                            selection={selection}
                                            executing={executingSelection}
                                            runningModuleIds={runningModuleIds}
                                            onExecute={onExecute}
                                            onExecuteRun={onExecuteRun}
                                            onRemove={savedSequences.toggle}
                                        />
                                    ))}
                                </ModuleTreeGroup>
                            )}
                            {groups.map(([groupName, modules]) => (
                                <ModuleTreeGroup
                                    key={groupName}
                                    name={groupName}
                                    icon={FolderCog}
                                    modules={modules}
                                    forceOpen={query !== ""}
                                    selection={selection}
                                    favorites={favorites}
                                    savedSequences={savedSequences}
                                    executingSelection={executingSelection}
                                    runningModuleIds={runningModuleIds}
                                    onExecute={onExecute}
                                    onExecuteSelection={onExecuteSelection}
                                />
                            ))}
                        </SidebarMenu>
                    </SidebarGroupContent>
                </SidebarGroup>
            </SidebarContent>
        </Sidebar>
    );
}

type SelectionActionsProps = {
    count: number;
    executing: boolean;
    onExecute: () => void;
};

function SelectionActions({ count, executing, onExecute }: SelectionActionsProps) {
    return (
        <div className="ml-auto flex min-w-0 items-center gap-2 group-data-[collapsible=icon]:hidden">
            <span className="truncate font-normal">{`${count} selected`}</span>
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button size="sm" className="h-6 gap-1 px-2 text-xs" disabled={executing} onClick={onExecute}>
                        {executing ? <LoaderCircle className="animate-spin" /> : <Play />}
                        Run all
                    </Button>
                </TooltipTrigger>
                <TooltipContent side="right">Run selected modules in the order they were selected</TooltipContent>
            </Tooltip>
        </div>
    );
}

type ModuleTreeGroupProps = {
    name: string;
    icon: LucideIcon;
    modules: ModuleMetadata[];
    forceOpen: boolean;
    selection: ModuleSelection;
    favorites: Favorites;
    savedSequences: SavedSequences;
    executingSelection: boolean;
    runningModuleIds: Set<string>;
    onExecute: (module: ModuleMetadata) => void;
    onExecuteSelection: () => void;
    hideFavoriteIcons?: boolean;
    children?: ReactNode;
};

function ModuleTreeGroup({
    name,
    icon: Icon,
    modules,
    forceOpen,
    selection,
    favorites,
    savedSequences,
    executingSelection,
    runningModuleIds,
    onExecute,
    onExecuteSelection,
    hideFavoriteIcons = false,
    children,
}: ModuleTreeGroupProps) {
    const { state, isMobile, setOpen: setSidebarOpen } = useSidebar();
    const [open, setOpen] = useState(true);

    const expandCollapsedSidebar = (event: MouseEvent) => {
        if (state === "collapsed" && !isMobile) {
            event.preventDefault();
            setOpen(true);
            setSidebarOpen(true);
        }
    };

    return (
        <Collapsible open={open || forceOpen} onOpenChange={setOpen} className="group/collapsible">
            <SidebarMenuItem>
                <CollapsibleTrigger asChild onClick={expandCollapsedSidebar}>
                    <SidebarMenuButton tooltip={name}>
                        <ChevronRight className="transition-transform group-data-[state=open]/collapsible:rotate-90 group-data-[collapsible=icon]:hidden" />
                        <Icon />
                        <span>{name}</span>
                    </SidebarMenuButton>
                </CollapsibleTrigger>
                <CollapsibleContent>
                    <SidebarMenuSub>
                        {children}
                        {modules.map((module) => (
                            <ModuleTreeItem
                                key={module.id}
                                module={module}
                                selection={selection}
                                favorites={favorites}
                                savedSequences={savedSequences}
                                showFavoriteIcon={!hideFavoriteIcons}
                                executingSelection={executingSelection}
                                runningModuleIds={runningModuleIds}
                                onExecute={onExecute}
                                onExecuteSelection={onExecuteSelection}
                            />
                        ))}
                    </SidebarMenuSub>
                </CollapsibleContent>
            </SidebarMenuItem>
        </Collapsible>
    );
}

type ModuleTreeItemProps = {
    module: ModuleMetadata;
    selection: ModuleSelection;
    favorites: Favorites;
    savedSequences: SavedSequences;
    showFavoriteIcon: boolean;
    executingSelection: boolean;
    runningModuleIds: Set<string>;
    onExecute: (module: ModuleMetadata) => void;
    onExecuteSelection: () => void;
};

function ModuleTreeItem({
    module,
    selection,
    favorites,
    savedSequences,
    showFavoriteIcon,
    executingSelection,
    runningModuleIds,
    onExecute,
    onExecuteSelection,
}: ModuleTreeItemProps) {
    const selectionIndex = selection.moduleIds.indexOf(module.id);
    const selectionCount = selection.moduleIds.length;
    const multiSelected = selectionIndex >= 0 && selectionCount > 1;
    const shown = module.id === selection.currentModuleId;
    const favorite = favorites.isFavorite(module.id);
    const favoriteIcon = favorite && showFavoriteIcon;
    const savedSelection = multiSelected && savedSequences.isSaved(selection.moduleIds);

    const onClick = (event: MouseEvent) => {
        if (event.ctrlKey || event.metaKey) {
            selection.toggle(module);
        } else {
            selection.select(module);
        }
    };

    return (
        <SidebarMenuSubItem>
            <ContextMenu>
                <Tooltip delayDuration={500}>
                    <ContextMenuTrigger asChild>
                        <TooltipTrigger asChild>
                            <SidebarMenuSubButton
                                asChild
                                isActive={shown}
                                className={cn(selectionIndex >= 0 && !shown && "bg-sidebar-primary/15")}
                            >
                                <button type="button" className="w-full" onClick={onClick}>
                                    {runningModuleIds.has(module.id) ? <RunStatusIcon status="RUNNING" /> : <Cog />}
                                    <span className="truncate">{getName(module)}</span>
                                    {(favoriteIcon || multiSelected) && (
                                        <span className="ml-auto flex shrink-0 items-center gap-1">
                                            {favoriteIcon && <Star className="size-3.5 fill-q-orange text-q-orange" />}
                                            {multiSelected && (
                                                <span
                                                    className={cn(
                                                        "rounded-full px-1.5 text-xs tabular-nums",
                                                        shown
                                                            ? "bg-sidebar-primary-foreground/20"
                                                            : "bg-sidebar-primary text-sidebar-primary-foreground",
                                                    )}
                                                >
                                                    {selectionIndex + 1}
                                                </span>
                                            )}
                                        </span>
                                    )}
                                </button>
                            </SidebarMenuSubButton>
                        </TooltipTrigger>
                    </ContextMenuTrigger>
                    {module.description && <TooltipContent side="right">{module.description}</TooltipContent>}
                </Tooltip>
                <ContextMenuContent>
                    {multiSelected && (
                        <>
                            <ContextMenuItem disabled={executingSelection} onSelect={onExecuteSelection}>
                                <Play />
                                {`Execute ${selectionCount} Selected Modules`}
                            </ContextMenuItem>
                            <ContextMenuItem onSelect={() => savedSequences.toggle(selection.moduleIds)}>
                                {savedSelection ? <ListX /> : <ListPlus />}
                                {savedSelection
                                    ? "Remove Selection from Saved Sequences"
                                    : "Save Selection as Sequence"}
                            </ContextMenuItem>
                            <ContextMenuSeparator />
                        </>
                    )}
                    <ContextMenuItem onSelect={() => onExecute(module)}>
                        <Play />
                        Execute Module
                    </ContextMenuItem>
                    <ContextMenuSeparator />
                    <ContextMenuItem onSelect={() => favorites.toggle(module.id)}>
                        {favorite ? (
                            <>
                                <StarOff />
                                Remove from Favorites
                            </>
                        ) : (
                            <>
                                <Star />
                                Add to Favorites
                            </>
                        )}
                    </ContextMenuItem>
                </ContextMenuContent>
            </ContextMenu>
        </SidebarMenuSubItem>
    );
}

function groupModules(modules: ModuleMetadata[]): [string, ModuleMetadata[]][] {
    const groups = new Map<string, ModuleMetadata[]>();
    for (const module of modules) {
        const groupName = getGroupName(module);
        groups.set(groupName, [...(groups.get(groupName) ?? []), module]);
    }

    return [...groups.entries()]
        .map(([groupName, groupModules]): [string, ModuleMetadata[]] => [groupName, groupModules.sort(compareModules)])
        .sort(([a], [b]) => a.localeCompare(b));
}

function matchesSourceFormat(module: ModuleMetadata, sourceFormat: string | undefined): boolean {
    return !sourceFormat || !module.sourceFormat || module.sourceFormat.toLowerCase() === sourceFormat.toLowerCase();
}

function matchesQuery(module: ModuleMetadata, query: string): boolean {
    return [getName(module), getGroupName(module)].some((value) => value.toLowerCase().includes(query));
}

function getGroupName(module: ModuleMetadata): string {
    const category = module.category.toLowerCase();
    return category === "parser" ? `${category} - ${(module.sourceFormat ?? "Other").toLowerCase()}` : category;
}

function compareModules(a: ModuleMetadata, b: ModuleMetadata): number {
    const orderA = a.order ?? Number.MAX_SAFE_INTEGER;
    const orderB = b.order ?? Number.MAX_SAFE_INTEGER;
    return orderA - orderB || getName(a).localeCompare(getName(b));
}
