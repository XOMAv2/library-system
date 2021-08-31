(ns service.library.db)
"
Входные данные библиотеки:
•	Название, не более 1024 символов.
•	Адрес, не более 1024 символов.
•	График работы по дням недели и праздникам, по 256 символов на каждую запись.

Library
---
Id: int
Uid: Guid
Name: string
Address: string
Schedule: string[]

LibraryBooks
---
Id: int
Uid: Guid
LibraryUid: Guid
BookUid: Guid
TotalQuantity: int ; Количество экземпляров конкретной книги на балансе конкретной библиотеки
GrantedQuantity: int ; Количество бронированных и/или выданных экземпляров конкретной книги в конкретной библиотеке
IsAvailable: bool

Order
---
Id: int
Uid: Guid
LibraryUid: Guid
BookUid: Guid
UserUid: Guid
BookingDate: DateTime
ReceivingDate: DateTime
ReturnDate: DateTime
Condition: string ; Состояние книги после возврата
"