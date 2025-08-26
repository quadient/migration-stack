import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card.tsx";

export default function ChartsSection() {
    return (
        <section className="flex-1 min-h-0 overflow-y-auto">
            <ChartCard />
        </section>
    );
}

function ChartCard() {
    return (
        <Card className="w-full max-w-sm h-100 border-gray-200">
            <CardHeader>
                <CardTitle className="text-xl">Assets Parsed</CardTitle>
            </CardHeader>
            <CardContent>
                <p>TODO later</p>
            </CardContent>
        </Card>
    );
}
