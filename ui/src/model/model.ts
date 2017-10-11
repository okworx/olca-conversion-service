export enum Format {
    ECOSPOLD_1 = "EcoSpold 1",
    JSON_LD = "JSON LD",
    ILCD = "ILCD",
}

export const FORMATS = [Format.ECOSPOLD_1, Format.ILCD, Format.JSON_LD];

export interface ConversionInfo {
    url: string;
    sourceFormat: Format;
    targetFormat: Format;
    error?: string;
    download?: string;
}
