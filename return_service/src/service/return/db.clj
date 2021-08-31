(ns service.return.db)
"
UserLimit
---
Id: int
Uid: Guid
UserUid: Guid
TotalLimit: int ; Максимальное число книг, доступных пользователю для бронирования/получения на руки
AvailableLimit: int ; Число книг, доступное пользователю в данный момент для бронирования/получения
"