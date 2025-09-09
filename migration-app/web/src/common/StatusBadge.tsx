import type { RunStatus } from "@/types/job.ts";
import { Badge } from "@/components/ui/badge.tsx";
import { CircleCheckBig, CircleX, LoaderCircle } from "lucide-react";

export function StatusBadge({ runStatus }: { runStatus: RunStatus }) {
    if (runStatus === "RUNNING") {
        return (
            <Badge className="h-6 bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-300">
                <>
                    <LoaderCircle className="animate-spin" />
                    Running
                </>
            </Badge>
        );
    } else if (runStatus === "SUCCESS") {
        return (
            <Badge className="h-6 bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-300">
                <>
                    <CircleCheckBig />
                    Success
                </>
            </Badge>
        );
    } else if (runStatus === "ERROR") {
        return (
            <Badge className="h-6 bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-300">
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
