import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useCallback, useState } from 'react';
import {
  listNotifications, markAsRead, getNotificationSettings,
  updateNotificationSettings, createNotificationStream,
  type NotificationListParams,
} from '../api/notifications';
import type { Notification, NotificationSettings } from '@reportplatform/types';

export function useNotifications(params: NotificationListParams = {}) {
  return useQuery({
    queryKey: ['notifications', params],
    queryFn: () => listNotifications(params),
  });
}

export function useMarkAsRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (ids: string[]) => markAsRead(ids),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });
}

export function useNotificationSettings() {
  return useQuery<NotificationSettings>({
    queryKey: ['notifications', 'settings'],
    queryFn: getNotificationSettings,
  });
}

export function useUpdateNotificationSettings() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (settings: NotificationSettings) => updateNotificationSettings(settings),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications', 'settings'] }),
  });
}

/** SSE hook for real-time notifications */
export function useSSE(baseUrl: string, token: string | null) {
  const [lastEvent, setLastEvent] = useState<Notification | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const qc = useQueryClient();

  const connect = useCallback(() => {
    if (!token) return;
    eventSourceRef.current = createNotificationStream(baseUrl, token);

    eventSourceRef.current.onmessage = (event) => {
      try {
        const notification = JSON.parse(event.data) as Notification;
        setLastEvent(notification);
        qc.invalidateQueries({ queryKey: ['notifications'] });
      } catch {
        // Ignore malformed events
      }
    };

    eventSourceRef.current.onerror = () => {
      eventSourceRef.current?.close();
      // Reconnect after 5 seconds
      setTimeout(connect, 5000);
    };
  }, [baseUrl, token, qc]);

  useEffect(() => {
    connect();
    return () => eventSourceRef.current?.close();
  }, [connect]);

  return { lastEvent };
}
