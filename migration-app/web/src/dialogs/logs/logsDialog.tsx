import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog.tsx";
import { ScrollArea } from "@/components/ui/scroll-area.tsx";
import { type ReactNode, useState } from "react";
import type { Job } from "@/sections/modulesSection/ModulesSection.tsx";
import { useRequest } from "@/hooks/useRequest.ts";

type LogsDialogProps = LogDialogBaseProps & {
    trigger: ReactNode;
};

type LogDialogContentProps = LogDialogBaseProps & {
    setOpen: (open: boolean) => void;
};

type LogDialogBaseProps = {
    moduleName: string;
    job: Job;
    setJobs: (value: ((prevState: Job[]) => Job[]) | Job[]) => void;
};

export default function LogsDialog({ trigger, moduleName, job, setJobs }: LogsDialogProps) {
    const [open, setOpen] = useState(false);

    return (
        <Dialog modal open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent style={{ width: "90%", height: "90%" }} className="sm:max-w-full p-10">
                <LogDialogOpenContent setOpen={setOpen} moduleName={moduleName} job={job} setJobs={setJobs} />
            </DialogContent>
        </Dialog>
    );
}

function LogDialogOpenContent({ moduleName, job, setJobs }: LogDialogContentProps) {
    useRequest<Job>({
        url: `api/job?id=${job.id}`,
        onSuccess: (job) => setJobs((prev) => prev.map((it) => (it.id === job.id ? job : it))),
        onError: (error) => console.error("Failed to fetch job logs:", error),
        condition: job.logs === undefined,
    });

    return (
        <>
            <DialogHeader>
                <DialogTitle>{`${moduleName} - Execution Log`}</DialogTitle>
                <DialogDescription className="text-muted-foreground">
                    Detailed log output from the last process execution
                </DialogDescription>
            </DialogHeader>
            <ScrollArea className="break-all overflow-y-auto pr-4">
                {job.logs?.map((log, idx) => (
                    <div key={idx} className="text-sm" style={{ marginBottom: "4px" }}>
                        {log}
                    </div>
                ))}
            </ScrollArea>
        </>
    );
}
