import { EmptyCard } from "@/common/EmptyCard.tsx";
import { CircleCheckBig, FileCog } from "lucide-react";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import type { UseFetchResult } from "@/hooks/useFetch.ts";
import {
    type ChartConfig,
    ChartContainer,
    ChartTooltip,
    ChartTooltipContent,
    ChartLegend,
    ChartLegendContent,
} from "@/components/ui/chart";
import { Label, Pie, PieChart } from "recharts";

export type TypeStatistics = {
    unsupportedCount: number | undefined;
    supportedCount: number | undefined;
};

type ChartsSectionProps = {
    statisticsResult: UseFetchResult<TypeStatistics>;
};

export default function ChartsSection({ statisticsResult }: ChartsSectionProps) {
    return (
        <section className="flex-1 min-h-0 overflow-y-auto">
            {statisticsResult.status === "ok" &&
            statisticsResult.data.supportedCount !== undefined &&
            statisticsResult.data.supportedCount !== null &&
            statisticsResult.data.unsupportedCount !== undefined &&
            statisticsResult.data.unsupportedCount !== null &&
            statisticsResult.data.supportedCount + statisticsResult.data.unsupportedCount !== 0 ? (
                <SupportedUnsupportedChart
                    supportedCount={statisticsResult.data.supportedCount}
                    unsupportedCount={statisticsResult.data.unsupportedCount}
                />
            ) : (
                <EmptyCard message={"Run any parser to see asset statistics"} icon={FileCog} />
            )}
        </section>
    );
}

function SupportedUnsupportedChart({
    supportedCount,
    unsupportedCount,
}: {
    supportedCount: number;
    unsupportedCount: number;
}) {
    const totalCount = supportedCount + unsupportedCount;
    const chartData = toChartData(supportedCount, unsupportedCount);
    return (
        <Card className="w-full max-w-sm min-w-75 h-[450px] flex flex-col">
            <CardHeader className="flex items-center gap-2">
                <div className="bg-muted rounded-xl p-2.5">
                    <CircleCheckBig className="w-6 h-6" />
                </div>
                <div>
                    <CardTitle>Assets Parsed</CardTitle>
                    <CardDescription>{`${totalCount} total assets`}</CardDescription>
                </div>
            </CardHeader>
            <CardContent className="flex-1 pb-0">
                <ChartContainer config={chartConfig} className="mx-auto aspect-square max-h-[250px]">
                    <PieChart>
                        <ChartLegend content={<ChartLegendContent />} />
                        <ChartTooltip cursor={false} content={<ChartTooltipContent hideLabel />} />
                        <Pie data={chartData} nameKey={"type"} dataKey={"count"} innerRadius={60} paddingAngle={2}>
                            <Label
                                content={({ viewBox }) => {
                                    if (viewBox && "cx" in viewBox && "cy" in viewBox) {
                                        return (
                                            <text
                                                x={viewBox.cx}
                                                y={viewBox.cy}
                                                textAnchor="middle"
                                                dominantBaseline="middle"
                                            >
                                                <tspan
                                                    x={viewBox.cx}
                                                    y={viewBox.cy}
                                                    className="text-2xl font-bold fill-chart-success"
                                                >
                                                    {((supportedCount / totalCount) * 100).toFixed(0)}%
                                                </tspan>
                                                <tspan
                                                    x={viewBox.cx}
                                                    y={(viewBox.cy || 0) + 24}
                                                    className="fill-muted-foreground"
                                                >
                                                    Supported
                                                </tspan>
                                            </text>
                                        );
                                    }
                                }}
                            />
                        </Pie>
                    </PieChart>
                </ChartContainer>
            </CardContent>
            <CardFooter className="flex justify-center items-center gap-x-20">
                <div className="flex flex-col items-center">
                    <div className="text-xl font-bold text-chart-success">{supportedCount}</div>
                    <div className="text-muted-foreground text-sm">Supported</div>
                </div>
                <div className="flex flex-col items-center">
                    <div className="text-xl font-bold text-destructive">{unsupportedCount}</div>
                    <div className="text-muted-foreground text-sm">Unsupported</div>
                </div>
            </CardFooter>
        </Card>
    );
}

function toChartData(supportedCount: number, unsupportedCount: number): chartData[] {
    return [
        {
            type: "supported",
            count: supportedCount,
            fill: "var(--chart-success)",
        },
        {
            type: "unsupported",
            count: unsupportedCount,
            fill: "var(--color-destructive)",
        },
    ];
}

const chartConfig = {
    supported: {
        label: "Supported",
        color: "var(--chart-success)",
    },
    unsupported: {
        label: "Unsupported",
        color: "var(--color-destructive)",
    },
} satisfies ChartConfig;

type chartData = {
    type: string;
    count: number;
    fill: string;
};
