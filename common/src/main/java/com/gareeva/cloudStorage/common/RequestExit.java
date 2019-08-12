package com.gareeva.cloudStorage.common;

/**
 * Это сообщение отправляется серверу при закрытии главного окна приложения.
 * Сервер в ответ отправляет это сообщение клиенту.
 * Клиент при получении сообщения прерывает поток чтения входящих данных.
 */
public class RequestExit extends AbstractMessage {

}
