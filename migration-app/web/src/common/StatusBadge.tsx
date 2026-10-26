import type { RunStatus } from "@/types/job.ts";
import { Badge } from "@/components/ui/badge.tsx";
import { CircleCheckBig, CircleX, LoaderCircle } from "lucide-react";

export function StatusBadge({ runStatus }: { runStatus: RunStatus }) {
    if (runStatus === "RUNNING") {
        return (
            <Badge className="h-6 bg-status-running-soft text-status-running">
                <>
                    <LoaderCircle className="animate-spin" />
                    Running
                </>
            </Badge>
        );
    } else if (runStatus === "SUCCESS") {
        return (
            <Badge className="h-6 bg-status-ok-soft text-status-ok">
                <>
                    <CircleCheckBig />
                    Success
                </>
            </Badge>
        );
    } else if (runStatus === "ERROR") {
        return (
            <Badge className="h-6 bg-status-fail-soft text-status-fail">
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
