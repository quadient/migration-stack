import type { UseFetchResult } from "@/hooks/useFetch.ts";

export type ModuleMetadata = {
    id: string;
    filename: string;
    category: string;
    displayName: string | undefined;
    sourceFormat: string | undefined;
    description: string | undefined;
    order: number | undefined;
};

export function getName(module: ModuleMetadata): string {
    return module.displayName || module.filename.replace(".groovy", "");
}

export function getSourceFormats(scriptsResult: UseFetchResult<ModuleMetadata[]>): string[] | undefined {
    return scriptsResult.status === "ok"
        ? ([...new Set(scriptsResult.data.map((script) => script.sourceFormat).filter(Boolean))] as string[])
        : undefined;
}
