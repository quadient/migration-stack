import { useState } from "react";

const SAVED_SEQUENCES_STORAGE_KEY = "saved_sequences";

export type SavedSequences = {
    sequences: string[][];
    isSaved: (moduleIds: string[]) => boolean;
    toggle: (moduleIds: string[]) => void;
};

export function useSavedSequences(): SavedSequences {
    const [sequences, setSequences] = useState(loadSequences);

    const isSaved = (moduleIds: string[]) => sequences.some((it) => sameSequence(it, moduleIds));

    const toggle = (moduleIds: string[]) => {
        const next = isSaved(moduleIds)
            ? sequences.filter((it) => !sameSequence(it, moduleIds))
            : [...sequences, moduleIds];
        setSequences(next);
        saveSequences(next);
    };

    return { sequences, isSaved, toggle };
}

export function sameSequence(a: string[], b: string[]): boolean {
    return a.length === b.length && a.every((id, index) => id === b[index]);
}

function loadSequences(): string[][] {
    try {
        const sequences: unknown = JSON.parse(localStorage.getItem(SAVED_SEQUENCES_STORAGE_KEY) ?? "[]");
        return Array.isArray(sequences) ? sequences.filter(isStringArray) : [];
    } catch {
        return [];
    }
}

function isStringArray(value: unknown): value is string[] {
    return Array.isArray(value) && value.every((it) => typeof it === "string");
}

function saveSequences(sequences: string[][]) {
    try {
        localStorage.setItem(SAVED_SEQUENCES_STORAGE_KEY, JSON.stringify(sequences));
    } catch {
        // ignore, not a big deal
    }
}
