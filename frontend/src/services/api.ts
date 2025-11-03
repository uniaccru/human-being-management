import axios, { AxiosResponse } from 'axios';
import { HumanBeing, CreateHumanBeingRequest, Car } from '../types';

// Для WildFly: если приложение деплоится на /human-being-manager/, используем относительный путь
const getApiBaseUrl = () => {
  const envUrl = (import.meta as any).env?.VITE_API_URL;
  if (envUrl) return envUrl;
  
  // Если запускаем на localhost (dev режим)
  if (window.location.hostname === 'localhost' && window.location.port !== '24180') {
    return 'http://localhost:8080/api';
  }
  
  // Для production на WildFly используем относительный путь
  // Если base path /human-being-manager/, то API будет /human-being-manager/api
  return '/human-being-manager/api';
};

const API_BASE_URL = getApiBaseUrl();

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Проверяем message (для стандартных ответов)
    if (error.response?.data?.message) {
      return Promise.reject(new Error(error.response.data.message));
    }
    // Проверяем error (для обратной совместимости)
    if (error.response?.data?.error) {
      return Promise.reject(new Error(error.response.data.error));
    }
    // Проверяем data (для объектов с детальной информацией)
    if (error.response?.data?.data?.errors) {
      const errors = error.response.data.data.errors;
      return Promise.reject(new Error('Validation errors: ' + (Array.isArray(errors) ? errors.join(', ') : errors)));
    }
    // Общая ошибка
    if (error.message) {
      return Promise.reject(new Error(error.message));
    }
    return Promise.reject(new Error('An unknown error occurred'));
  }
);

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export class HumanBeingApi {
  
  static async getAll(): Promise<HumanBeing[]> {
    const response: AxiosResponse<HumanBeing[]> = await apiClient.get('/humanbeings');
    return response.data;
  }

  static async getAllPaginated(page: number = 0, size: number = 10): Promise<PaginatedResponse<HumanBeing>> {
    const response: AxiosResponse<PaginatedResponse<HumanBeing>> = await apiClient.get(`/humanbeings?page=${page}&size=${size}`);
    return response.data;
  }

  static async getAllPaginatedWithFilters(
    page: number = 0, 
    size: number = 10,
    filterColumn?: string,
    filterValue?: string,
    sortColumn?: string,
    sortDirection: string = 'asc'
  ): Promise<PaginatedResponse<HumanBeing>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortDirection
    });
    
    if (filterColumn && filterValue) {
      params.append('filterColumn', filterColumn);
      params.append('filterValue', filterValue);
    }
    
    if (sortColumn) {
      params.append('sortColumn', sortColumn);
    }
    
    const response: AxiosResponse<PaginatedResponse<HumanBeing>> = await apiClient.get(`/humanbeings?${params.toString()}`);
    return response.data;
  }

  static async getById(id: number): Promise<HumanBeing> {
    const response: AxiosResponse<HumanBeing> = await apiClient.get(`/humanbeings/${id}`);
    return response.data;
  }


  static async create(humanBeing: CreateHumanBeingRequest): Promise<HumanBeing> {
    const response: AxiosResponse<HumanBeing> = await apiClient.post('/humanbeings', humanBeing);
    return response.data;
  }


  static async update(id: number, humanBeing: Partial<HumanBeing>): Promise<HumanBeing> {
    const response: AxiosResponse<HumanBeing> = await apiClient.put(`/humanbeings/${id}`, humanBeing);
    return response.data;
  }


  static async delete(id: number): Promise<void> {
    await apiClient.delete(`/humanbeings/${id}`);
  }

  static async getCount(): Promise<number> {
    const response: AxiosResponse<{count: number}> = await apiClient.get('/humanbeings/count');
    return response.data.count;
  }


  static async searchByName(name: string): Promise<HumanBeing[]> {
    const response: AxiosResponse<HumanBeing[]> = await apiClient.get(`/humanbeings/search?name=${encodeURIComponent(name)}`);
    return response.data;
  }

  static async getAllCars(): Promise<Car[]> {
    const response: AxiosResponse<Car[]> = await apiClient.get('/humanbeings/cars');
    return response.data;
  }
}

export class SpecialOperationsApi {
  
  static async getSumOfMinutesWaiting(): Promise<number> {
    const response: AxiosResponse<{sum: number}> = await apiClient.get('/special-operations/sum-minutes-waiting');
    return response.data.sum;
  }


  static async getMaxToothpick(): Promise<HumanBeing> {
    const response: AxiosResponse<HumanBeing> = await apiClient.get('/special-operations/max-toothpick');
    return response.data;
  }

  static async getSoundtrackStartsWith(substring: string): Promise<HumanBeing[]> {
    const response: AxiosResponse<HumanBeing[]> = await apiClient.get(`/special-operations/soundtrack-starts-with?substring=${encodeURIComponent(substring)}`);
    return response.data;
  }


  static async deleteHeroesWithoutToothpicks(): Promise<number> {
    const response: AxiosResponse<{deletedCount: number}> = await apiClient.delete('/special-operations/delete-heroes-without-toothpicks');
    return response.data.deletedCount;
  }

  static async setAllMoodToSadness(): Promise<number> {
    const response: AxiosResponse<{updatedCount: number}> = await apiClient.put('/special-operations/set-all-mood-sadness');
    return response.data.updatedCount;
  }
}

export interface ImportResult {
  success: boolean;
  message: string;
  data: {
    totalProcessed: number;
    successfullyImported: number;
    failed: number;
    errors?: string[];
  };
}

export interface ImportHistory {
  id: number;
  status: string;
  username: string;
  addedCount: number | null;
  totalProcessed: number;
  failedCount: number;
  errorMessage: string | null;
  createdAt: string;
}

export class ImportApi {
  static async importHumanBeings(humanBeings: CreateHumanBeingRequest[]): Promise<ImportResult> {
    const response: AxiosResponse<ImportResult> = await apiClient.post('/import/humanbeings', humanBeings);
    return response.data;
  }

  static async getImportHistory(): Promise<ImportHistory[]> {
    const response: AxiosResponse<ImportHistory[]> = await apiClient.get('/import/history');
    return response.data;
  }
}

export default HumanBeingApi;