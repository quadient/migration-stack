import { EmptyCard } from "@/common/EmptyCard.tsx";
import { FileCog } from "lucide-react";
import { Card } from "@/components/ui/card.tsx";
import type { UseFetchResult } from "@/hooks/useFetch.ts";

export type TypeStatistics = {
    unsupportedCount: number | null;
    supportedCount: number | null;
};

type ChartsSectionProps = {
    statisticsResult: UseFetchResult<TypeStatistics>;
};

export default function ChartsSection({ statisticsResult }: ChartsSectionProps) {
    return (
        <section className="flex-1 min-h-0 overflow-y-auto">
            {statisticsResult.status === "ok" &&
            statisticsResult.data.supportedCount !== null &&
            statisticsResult.data.unsupportedCount !== null ? (
                <Card className="w-full max-w-sm min-w-75 h-75 flex flex-col justify-center items-center">
                    <div className="flex-1">{`${((statisticsResult.data.supportedCount / (statisticsResult.data.supportedCount + statisticsResult.data.unsupportedCount)) * 100).toFixed(2)} %`}</div>
                    <div className="flex-1">{`${((statisticsResult.data.unsupportedCount / (statisticsResult.data.supportedCount + statisticsResult.data.unsupportedCount)) * 100).toFixed(2)} %`}</div>
                </Card>
            ) : (
                <EmptyCard message={"Run any parser to see asset statistics"} icon={FileCog} />
            )}
        </section>
    );
}
