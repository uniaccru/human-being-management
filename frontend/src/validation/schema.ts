import * as yup from 'yup';
import { Mood, WeaponType } from '../types';

export const humanBeingSchema = yup.object().shape({
  name: yup
    .string()
    .required('Name is required')
    .min(1, 'Name cannot be empty')
    .max(100, 'Name must be 100 characters or less')
    .matches(/^[a-zA-Z0-9\s\-_.]+$/, 'Name can only contain letters, numbers, spaces, hyphens, underscores, and periods'),
  
  coordinates: yup.object().shape({
    x: yup
      .number()
      .required('X coordinate is required')
      .integer('X must be an integer')
      .min(-1000, 'X coordinate must be at least -1000')
      .max(1000, 'X coordinate must be at most 1000')
      .test('not-zero', 'X coordinate cannot be zero', value => value !== 0),
    
    y: yup
      .number()
      .required('Y coordinate is required')
      .min(-965, 'Y coordinate must be greater than -965')
      .max(1000, 'Y coordinate must be at most 1000')
      .test('not-zero', 'Y coordinate cannot be zero', value => value !== 0)
  }),
  
  realHero: yup.boolean().required('Real hero status is required'),
  
  hasToothpick: yup.boolean().nullable().optional(),
  
  car: yup.object().shape({
    name: yup
      .string()
      .nullable()
      .max(50, 'Car name must be 50 characters or less')
      .matches(/^[a-zA-Z0-9\s\-_.]*$/, 'Car name can only contain letters, numbers, spaces, hyphens, underscores, and periods'),
    cool: yup.boolean().required('Car cool status is required')
  }),
  
  mood: yup
    .string()
    .oneOf(Object.values(Mood), 'Please select a valid mood')
    .required('Mood is required'),
  
  impactSpeed: yup
    .number()
    .required('Impact speed is required')
    .min(-1000, 'Impact speed must be at least -1000')
    .max(1000, 'Impact speed must be at most 1000')
    .test('real-hero-speed', 'Real heroes cannot have negative impact speed', function(value) {
      const { realHero } = this.parent;
      if (realHero && value < 0) {
        return this.createError({ message: 'Real heroes cannot have negative impact speed' });
      }
      return true;
    }),
  
  soundtrackName: yup
    .string()
    .required('Soundtrack name is required')
    .min(1, 'Soundtrack name cannot be empty')
    .max(100, 'Soundtrack name must be 100 characters or less')
    .matches(/^[a-zA-Z0-9\s\-_.]+$/, 'Soundtrack name can only contain letters, numbers, spaces, hyphens, underscores, and periods'),
  
  minutesOfWaiting: yup
    .number()
    .required('Minutes of waiting is required')
    .min(0, 'Minutes of waiting cannot be negative')
    .max(99999, 'Minutes of waiting must be less than 100,000')
    .integer('Minutes of waiting must be a whole number'),
  
  weaponType: yup
    .string()
    .oneOf(Object.values(WeaponType), 'Please select a valid weapon type')
    .required('Weapon type is required')
});

export const validateInput = {
  nameInput: (value: string) => {
    return value.replace(/[^a-zA-Z0-9\s\-_.]/g, '');
  },
  
  numberInput: (value: string) => {
    return value.replace(/[^0-9.-]/g, '');
  },
  
  integerInput: (value: string) => {
    return value.replace(/[^0-9-]/g, '');
  },

  limitLength: (value: string, maxLength: number) => {
    return value.slice(0, maxLength);
  }
};

export const validateField = {
  name: (value: string) => {
    if (!value || value.trim().length === 0) {
      return 'Name is required';
    }
    if (value.length > 100) {
      return 'Name must be 100 characters or less';
    }
    if (!/^[a-zA-Z0-9\s\-_.]+$/.test(value)) {
      return 'Name can only contain letters, numbers, spaces, hyphens, underscores, and periods';
    }
    return null;
  },
  
  coordinateX: (value: number) => {
    if (isNaN(value)) {
      return 'X coordinate must be a number';
    }
    if (!Number.isInteger(value)) {
      return 'X must be an integer';
    }
    if (value < -1000 || value > 1000) {
      return 'X coordinate must be between -1000 and 1000';
    }
    if (value === 0) {
      return 'X coordinate cannot be zero';
    }
    return null;
  },
  
  coordinateY: (value: number) => {
    if (isNaN(value)) {
      return 'Y coordinate must be a number';
    }
    if (value <= -965) {
      return 'Y coordinate must be greater than -965';
    }
    if (value > 1000) {
      return 'Y coordinate must be at most 1000';
    }
    if (value === 0) {
      return 'Y coordinate cannot be zero';
    }
    return null;
  },
  
  impactSpeed: (value: number, realHero: boolean) => {
    if (isNaN(value)) {
      return 'Impact speed must be a number';
    }
    if (value < -1000 || value > 1000) {
      return 'Impact speed must be between -1000 and 1000';
    }
    if (realHero && value < 0) {
      return 'Real heroes cannot have negative impact speed';
    }
    return null;
  },
  
  minutesOfWaiting: (value: number) => {
    if (isNaN(value)) {
      return 'Minutes of waiting must be a number';
    }
    if (!Number.isInteger(value)) {
      return 'Minutes of waiting must be a whole number';
    }
    if (value < 0) {
      return 'Minutes of waiting cannot be negative';
    }
    if (value > 99999) {
      return 'Minutes of waiting must be less than 100,000';
    }
    return null;
  }
};
