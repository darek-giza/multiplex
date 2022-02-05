###To run the multiplex API
Please go to the terminal and execute ``./runMultiplex.sh`` in the root directory. <br>
After a few seconds, SBT should start the API.

###The API creates the default data:
- screening rooms "A", "B", "C",
- in each room, screenings are held at 2 p.m., 4 p.m., 6 p.m. and 8 p.m.
- we schedule projections for the next seven days

For test API you can use your browser to get requests and Postman to Post data.

###Provided endpoints:
- GET list movies from startAt in millis as [Long] to finishAt in millis as [Long] <br>
http://localhost:8080/movies?start=${startAt}&finish=${endAt}
- GET projection details (by projectionId) <br>
http://localhost:8080/movies/details?id=${projectionId}
- POST order - needs orderDto as body: <br>
http://localhost:8080/order

Example orderDto:
```
{
    "id": "1234-1234-1234-1234",
    "places": [
        {
            "place": {
                "row": 1,
                "seat": 1"
            },
            "ticket": "ADULT"
        }
    ],
    "name": "Dariusz Giza"
}
```
#####You can make order for one or more place but please remember,that
```
choose only first free places in each row, for example:
row 1 - free place 2, 3, 4   == Place(1,2), Place(1,3), Place(1,4)
row 2 - free place 3, 4, 5   == Place(2,3), Place(2,4), Place(2,5)

If you want to choose:
 - one place: 
   Place(1,2) or Place(2,3)

 - two places:
   Place(1,2), Place(1,3)  or  Place(1,2), Place(2,3)

 - three places:
   Place(1,2), Place(1,3), Place(1,4)  or  Place(1,2), Place(1,3), Place(2,3)
```

###To see how API make a few requests using curl
Open new terminal window and execute
```
./example.sh
```
The script will execute as follows
1. take a  list of today's movies
2. choose the first movie from the list
3. get details of the chosen movie
4. in the loop make 10 times <br>
   -get first free place <br>
   -make post order request to reserve a place
5. get projection details