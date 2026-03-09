import apiClient from './axios';
import type {
  Notification,
  NotificationSettings,
  PaginatedResponse,
  PaginationParams,
} from '@reportplatform/types';

export interface NotificationListParams extends PaginationParams {
  read?: boolean;
}

export async function listNotifications(params: NotificationListParams = {}): Promise<PaginatedResponse<Notification>> {
  const { data } = await apiClient.get<PaginatedResponse<Notification>>('/notifications', { params });
  return data;
}

export async function markAsRead(notificationIds: string[]): Promise<void> {
  await apiClient.put('/notifications', { notification_ids: notificationIds });
}

export async function getNotificationSettings(): Promise<NotificationSettings> {
  const { data } = await apiClient.get<NotificationSettings>('/notifications/settings');
  return data;
}

export async function updateNotificationSettings(settings: NotificationSettings): Promise<NotificationSettings> {
  const { data } = await apiClient.put<NotificationSettings>('/notifications/settings', settings);
  return data;
}

/** Create an SSE EventSource for real-time notifications */
export function createNotificationStream(baseUrl: string, token: string): EventSource {
  const url = new URL('/api/v1/notifications/stream', baseUrl);
  url.searchParams.set('token', token);
  return new EventSource(url.toString());
}
