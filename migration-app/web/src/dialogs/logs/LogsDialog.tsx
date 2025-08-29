import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog.tsx";
import { ScrollArea } from "@/components/ui/scroll-area.tsx";
import { type ReactNode, useEffect, useRef } from "react";
import { useRequest } from "@/hooks/useRequest.ts";
import { Card, CardContent } from "@/components/ui/card.tsx";
import type { Job } from "@/types/job.ts";
import { StatusBadge } from "@/common/StatusBadge.tsx";

type LogsDialogProps = LogDialogBaseProps & {
    trigger: ReactNode;
};

type LogDialogBaseProps = {
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
            <DialogContent style={{ width: "90%", height: "90%" }} className="grid grid-rows-[auto_1fr] sm:max-w-full">
                <LogDialogOpenContent
                    moduleName={moduleName}
                    job={job}
                    setJobs={setJobs}
                    open={open}
                    setOpen={setOpen}
                />
            </DialogContent>
        </Dialog>
    );
}

function LogDialogOpenContent({ moduleName, job, setJobs }: LogDialogBaseProps) {
    useRequest<Job>({
        url: `api/job?id=${job.id}`,
        onSuccess: (job) => setJobs((prev) => prev.map((it) => (it.id === job.id ? job : it))),
        onError: (error) => console.error("Failed to fetch job logs:", error),
        condition: job.logs === undefined,
    });

    const scrollAreaRef = useRef<HTMLDivElement>(null);
    useEffect(() => {
        if (!scrollAreaRef.current) {
            return;
        }

        const viewport = scrollAreaRef.current.querySelector("[data-radix-scroll-area-viewport]");
        if (!viewport) {
            return;
        }

        if (viewport.scrollHeight - viewport.clientHeight - viewport.scrollTop > viewport.clientHeight) {
            return;
        }

        viewport.scrollTop = viewport.scrollHeight;
    }, [job?.logs]);

    return (
        <>
            <DialogHeader>
                <DialogTitle>{`${moduleName} - Execution Log`}</DialogTitle>
                <DialogDescription className="text-muted-foreground">
                    Detailed log output from the last process execution
                </DialogDescription>
            </DialogHeader>
            <Card className="flex flex-col h-full w-full overflow-hidden py-2">
                <CardContent className="flex flex-1 overflow-hidden px-2">
                    <ScrollArea
                        ref={scrollAreaRef}
                        style={{ overflowWrap: "break-word", wordBreak: "break-word" }}
                        className="pr-4"
                    >
                        {job.logs?.map((log, idx) => (
                            <div key={idx} className="text-sm" style={{ marginBottom: "4px" }}>
                                {log}
                            </div>
                        ))}
                    </ScrollArea>
                </CardContent>
            </Card>
            <DialogFooter className="sm:justify-start">
                <StatusBadge runStatus={job.status} />
            </DialogFooter>
        </>
    );
}
