import { Button } from "@/components/ui/button.tsx";
import { Card, CardAction, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import { ScrollArea } from "@/components/ui/scroll-area";
import { FileCog, Rocket, Play, LoaderCircle, FileText, CircleCheckBig, CircleX } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { EmptyCard } from "@/common/emptyCard.tsx";
import LogsDialog from "@/dialogs/logs/logsDialog.tsx";
import { Badge } from "@/components/ui/badge.tsx";

export type ModuleMetadata = {
    filename: string;
    category: string;
    path: string;
    displayName: string | undefined;
    sourceFormat: string | undefined;
    description: string | undefined;
};

type RunStatus = "running" | "success" | "error";

export type RunResult = {
    path: string;
    name: string;
    status: RunStatus;
    lastUpdated: Date;
    logs: string[];
};

export type ScriptRunResultsMap = Map<string, RunResult>;

type SetRunResults = (value: (prev: ScriptRunResultsMap) => ScriptRunResultsMap) => void;

type ModulesSectionProp = {
    modules: ModuleMetadata[];
    sourceFormat: string | undefined;
    scriptRunResults: ScriptRunResultsMap;
    setScriptRunResults: SetRunResults;
};

export default function ModulesSection({
    modules,
    sourceFormat,
    scriptRunResults,
    setScriptRunResults,
}: ModulesSectionProp) {
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
                                        runResult={scriptRunResults.get(module.path)}
                                        setRunResults={setScriptRunResults}
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
                                        runResult={scriptRunResults.get(module.path)}
                                        setRunResults={setScriptRunResults}
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
    runResult: RunResult | undefined;
    setRunResults: SetRunResults;
};

function ModuleCard({ module, icon: Icon, runResult, setRunResults }: ModuleCardProps) {
    return (
        <Card className="w-full max-w-sm min-w-75 h-75 flex flex-col" key={module.filename}>
            <CardHeader>
                <CardTitle className="flex items-center gap-2 text-lg font-normal max-w-50">
                    <div className="bg-muted rounded-xl p-2.5">
                        <Icon className="w-6 h-6" />
                    </div>
                    {getName(module)}
                </CardTitle>
                {!!runResult && (
                    <CardAction>
                        <StatusBadge runStatus={runResult.status} />
                    </CardAction>
                )}
            </CardHeader>
            <CardContent className="text-muted-foreground">{module.description}</CardContent>
            <CardFooter className="flex flex-col gap-4 justify-center mt-auto">
                {(runResult?.status === "success" || runResult?.status === "error") && (
                    <div className="flex justify-between items-center w-full">
                        <div className="text-muted-foreground text-xs">
                            {`Last run: ${runResult.lastUpdated.toLocaleString()}`}
                        </div>
                        <LogsDialog
                            trigger={
                                <Button className="text-muted-foreground text-xs" variant={"ghost"}>
                                    <FileText className="text-muted-foreground" />
                                    View logs
                                </Button>
                            }
                            runResult={runResult}
                        />
                    </div>
                )}
                <Button className="w-50" type={"submit"} onClick={() => handleExecuteModule(module, setRunResults)}>
                    {runResult?.status === "running" ? (
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
    if (runStatus === "running") {
        return (
            <Badge className="bg-blue-100 text-blue-800">
                <>
                    <LoaderCircle className="animate-spin" />
                    Running
                </>
            </Badge>
        );
    } else if (runStatus === "success") {
        return (
            <Badge className="bg-green-100 text-green-800">
                <>
                    <CircleCheckBig />
                    Success
                </>
            </Badge>
        );
    } else if (runStatus === "error") {
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

async function handleExecuteModule(module: ModuleMetadata, setRunResults: SetRunResults): Promise<void> {
    const path = module.path;
    const name = getName(module);

    setRunResults((prev) =>
        new Map(prev).set(path, { path, name, status: "running", lastUpdated: new Date(), logs: [] }),
    );

    try {
        const response = await fetch("/api/scripts/run", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ path }),
        });
        if (!response.ok) {
            const errorMessage = await response.text();
            const errorLog = `Module execution failed. Http status: ${response.status}. Error message: ${errorMessage}.`;
            setRunResults((prev) =>
                new Map(prev).set(path, { path, name, status: "error", lastUpdated: new Date(), logs: [errorLog] }),
            );
            return;
        }

        const { logs } = await response.json();

        setRunResults((prev) =>
            new Map(prev).set(path, { path, name, status: "success", lastUpdated: new Date(), logs }),
        );
    } catch (error) {
        console.error("Error executing module:", error);
        setRunResults((prev) =>
            new Map(prev).set(path, { path, name, status: "error", lastUpdated: new Date(), logs: [String(error)] }),
        );
    }
}

function getName(module: ModuleMetadata): string {
    return module.displayName || module.filename.replace(".groovy", "");
}
