package com.tienda.pedidos.service;

// Abstraccion del envio de la confirmacion: el servicio de pedidos no sabe si es correo, SMS o un mock
public interface NotificacionService {

    void enviarConfirmacion(String clienteEmail, double total);
}
