import { useState } from "react";
import { LoaderCircle, Play } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { executeModules } from "@/common/executeModules.ts";
import ErrorDialog from "@/dialogs/error/ErrorDialog.tsx";
import type { ModuleMetadata } from "@/types/moduleMetadata.ts";
import type { Job } from "@/types/job.ts";

type ExecuteModuleButtonProps = {
    module: ModuleMetadata;
    job: Job | undefined;
    setJobs: (value: ((prev: Job[]) => Job[]) | Job[]) => void;
    onStarted?: () => void;
    onJobFinished?: () => void;
    className?: string;
};

export function ExecuteModuleButton({
    module,
    job,
    setJobs,
    onStarted,
    onJobFinished,
    className,
}: ExecuteModuleButtonProps) {
    const [error, setError] = useState<string>();

    const execute = async () => {
        setError(undefined);
        setError(await executeModules([module.id], setJobs, { onJobStarted: onStarted, onJobFinished }));
    };

    return (
        <>
            <Button className={className} type={"submit"} disabled={job?.status === "RUNNING"} onClick={execute}>
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
            <ErrorDialog title="Module could not be started" error={error} onClose={() => setError(undefined)} />
        </>
    );
}
