export type SettingsFormProps = {
    settings: Settings;
    setSettings: (value: ((prevState: Settings) => Settings) | Settings) => void;
};

export type Settings = {
    projectConfig: ProjectConfig;
    migrationConfig: MigrationConfig;
    sourceFormat?: string;
};

export type MigrationConfig = {
    dbConfig: DbConfig;
    inspireConfig: InspireConfig;
    storageRoot?: string | undefined;
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
    styleDefinitionPath: string | undefined;
    inputDataPath: string;
    interactiveTenant: string;
    defaultTargetFolder?: string;
    paths: PathsConfig;
    inspireOutput: InspireOutput;
    sourceBaseTemplatePath?: string;
    defaultVariableStructure?: string;
    defaultLanguage?: string;
    context: Record<string, any>;
};

export type PathsConfig = {
    images?: string | undefined;
    fonts?: string | undefined;
};

export const inspireOutputOptions = ["Designer", "Interactive", "Evolve"] as const;
export type InspireOutput = (typeof inspireOutputOptions)[number];
