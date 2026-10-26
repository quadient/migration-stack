import { useState } from "react";

const FAVORITES_STORAGE_KEY = "favorite_modules";

export type Favorites = {
    moduleIds: string[];
    isFavorite: (moduleId: string) => boolean;
    toggle: (moduleId: string) => void;
};

export function useFavorites(): Favorites {
    const [moduleIds, setModuleIds] = useState(loadFavorites);

    const toggle = (moduleId: string) => {
        const next = moduleIds.includes(moduleId)
            ? moduleIds.filter((id) => id !== moduleId)
            : [...moduleIds, moduleId];
        setModuleIds(next);
        saveFavorites(next);
    };

    return {
        moduleIds,
        isFavorite: (moduleId) => moduleIds.includes(moduleId),
        toggle,
    };
}

function loadFavorites(): string[] {
    try {
        const moduleIds: unknown = JSON.parse(localStorage.getItem(FAVORITES_STORAGE_KEY) ?? "[]");
        return Array.isArray(moduleIds) ? moduleIds.filter((id) => typeof id === "string") : [];
    } catch {
        return [];
    }
}

function saveFavorites(moduleIds: string[]) {
    try {
        localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(moduleIds));
    } catch {
        // ignore, not a big deal
    }
}
