export type SettingsFormProps = {
    settings: Settings;
    setSettings: (value: ((prevState: Settings) => Settings) | Settings) => void;
}

export type Settings = {
    projectConfig: ProjectConfig;
    migrationConfig: MigrationConfig;
};

export type MigrationConfig = {
    dbConfig: DbConfig;
    inspireConfig: InspireConfig;
    storageRoot?: string | null;
};

export type DbConfig = {
    host: string;
    port: number;
    dbName: string;
    user: string;
    password: string;
};

export type InspireConfig = {
    ipsConfig: IpsConfig;
};

export type IpsConfig = {
    host: string;
    port: number;
    timeoutSeconds: number;
};

export type ProjectConfig = {
    name: string;
    baseTemplatePath: string;
    inputDataPath: string;
    interactiveTenant: string;
    defaultTargetFolder?: string | null;
    paths: PathsConfig;
    inspireOutput: InspireOutput;
    sourceBaseTemplatePath?: string | null;
    defaultVariableStructure?: string | null;
};

export type PathsConfig = {
    images?: string | null;
};

export const inspireOutputOptions = ["Designer", "Interactive", "Evolve"] as const;
export type InspireOutput = (typeof inspireOutputOptions)[number];
