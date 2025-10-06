
export enum Mood {
  SADNESS = 'SADNESS',
  APATHY = 'APATHY',
  CALM = 'CALM',
  RAGE = 'RAGE'
}

export enum WeaponType {
  AXE = 'AXE',
  SHOTGUN = 'SHOTGUN',
  MACHINE_GUN = 'MACHINE_GUN'
}

export interface Coordinates {
  x: number;
  y: number;
}

export interface Car {
  id?: number;
  name?: string;
  cool: boolean;
}

export interface HumanBeing {
  id?: number;
  name: string;
  coordinates: Coordinates;
  creationDate?: Date;
  realHero: boolean;
  hasToothpick?: boolean | null;
  car: Car;
  mood: Mood;
  impactSpeed: number;
  soundtrackName: string;
  minutesOfWaiting: number;
  weaponType: WeaponType;
}

export interface ApiError {
  error: string;
  timestamp: number;
}

export interface CreateHumanBeingRequest {
  name: string;
  coordinates: Coordinates;
  realHero: boolean;
  hasToothpick?: boolean | null;
  car: Car;
  mood: Mood;
  impactSpeed: number;
  soundtrackName: string;
  minutesOfWaiting: number;
  weaponType: WeaponType;
}