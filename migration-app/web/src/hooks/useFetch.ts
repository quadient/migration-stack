import React from "react";

export type SuccessFetchResult<T> = {
    data: T;
    setData: (value: ((prev: T) => T) | T) => void;
    status: "ok";
};

export type UseFetchResult<T> = (
    | {
          status: "error";
          error: unknown;
      }
    | {
          status: "loading";
      }
    | SuccessFetchResult<T>
) & { refetch: () => void };

export function useFetch<T>(url: string, requestInit?: RequestInit): UseFetchResult<T> {
    const [status, setStatus] = React.useState("loading");
    const [error, setError] = React.useState<unknown | null>(undefined);
    const [data, setData] = React.useState<T | undefined>(undefined);
    const [fetchCount, setFetchCount] = React.useState(0);

    const buildInit = React.useEffectEvent((signal: AbortSignal): RequestInit => ({
        ...requestInit,
        method: requestInit?.method || "GET",
        signal,
    }));

    const refetch = React.useCallback(() => setFetchCount((count) => count + 1), []);

    React.useEffect(() => {
        let cancelled = false;
        const controller = new AbortController();

        fetch(url, buildInit(controller.signal))
            .then((result) => {
                if (cancelled) {
                    return;
                }
                result
                    .json()
                    .then((json) => {
                        if (cancelled) {
                            return;
                        }
                        setData(json as T);
                        setStatus("ok");
                    })
                    .catch((err) => {
                        if (cancelled) {
                            return;
                        }
                        setStatus("error");
                        setError(err);
                    });
            })
            .catch((err) => {
                if (cancelled) {
                    return;
                }
                setStatus("error");
                setError(err);
            });

        return () => {
            controller.abort();
            cancelled = true;
        };
    }, [url, fetchCount]);

    return { data, setData, error, status, refetch } as UseFetchResult<T>;
}
