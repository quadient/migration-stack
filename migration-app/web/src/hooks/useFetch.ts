import React from "react";

type UseFetchResult<T> =
    | {
          status: "error";
          error: unknown;
      }
    | {
          status: "loading";
      }
    | {
          data: T;
          setData: (value: ((prev: T) => T) | T) => void;
          status: "ok";
      };

export function useFetch<T>(url: string, defaultValue: T, requestInit?: RequestInit): UseFetchResult<T> {
    const [status, setStatus] = React.useState("loading");
    const [error, setError] = React.useState<unknown | null>(undefined);
    const [data, setData] = React.useState<T>(defaultValue);

    React.useEffect(() => {
        let cancelled = false;
        const controller = new AbortController();
        const init: RequestInit = requestInit ?? {};
        init.signal = controller.signal;
        if (!init.method) {
            init.method == "GET";
        }

        fetch(url, init)
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
                        setData(json);
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
    }, [url]);

    return { data, setData, error, status } as UseFetchResult<T>;
}
