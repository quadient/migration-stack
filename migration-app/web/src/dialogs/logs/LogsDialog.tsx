import type { ReactNode } from "react";
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { JobLogs } from "@/common/JobLogs.tsx";
import { StatusBadge } from "@/common/StatusBadge.tsx";
import type { Job } from "@/types/job.ts";

type LogsDialogProps = {
    trigger: ReactNode;
    moduleName: string;
    job: Job;
    setJobs: (value: ((prevState: Job[]) => Job[]) | Job[]) => void;
    open: boolean;
    setOpen: (open: boolean) => void;
};

export default function LogsDialog({ trigger, moduleName, job, setJobs, open, setOpen }: LogsDialogProps) {
    return (
        <Dialog modal open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent className="h-11/12 w-11/12 grid grid-rows-[auto_1fr_auto] sm:max-w-full">
                <DialogHeader>
                    <DialogTitle>{`${moduleName} - Execution Log`}</DialogTitle>
                    <DialogDescription className="text-muted-foreground">
                        Detailed log output from the last process execution
                    </DialogDescription>
                </DialogHeader>
                <div className="min-h-0">
                    <JobLogs job={job} setJobs={setJobs} />
                </div>
                <DialogFooter className="sm:justify-between items-center">
                    <StatusBadge runStatus={job.status} />
                    <DialogClose asChild>
                        <Button variant="outline">Cancel</Button>
                    </DialogClose>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
