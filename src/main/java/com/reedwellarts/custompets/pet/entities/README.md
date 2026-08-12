# Adding a pet entity
### There are 6 steps required to add an entity to the pet system
1. Create a pet entity class and implement all functions in OwnablePet and InventoryOwner as well as all living entity functions the delegate uses
2. Override goal initialization and rebuild the AI goal set, adding in any custom goals
3. Create pet type initialization in ModEntities class
4. Create a converter for the pet and add it to the map in TameEntityEventHandler
5. Register attributes with fabric attribute registery in CustomPets main server class
6. Register base mob renderer for the mob entity in CustomPetsClient main client class
### Once these steps are completed, be sure to test and make sure the new pet type functions correctly
