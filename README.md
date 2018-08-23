# 'Erbil' == reverse('librE')

Freestyle Libre is a wearable sensor that measures the glucose level through the blood plasma.

As a diabetic one is currently offered two options of retrieving the data from the wearable sensor: 

1. Always carry an extra device with dimentions of a really old mobile phone, that is buggy and runs out of battery. 
2. Use your mobile phone, but accept some really shitty user agreements where you don't own your data.

Well, I have chosen another path. Reverse Enginnering. My plan is to find out how the sensor works, and build an android app that extracts and stores the data _locally_.  

> Erbil aims to increase accessibility of bloodsugar data for diabetics using a Free Style Libre sensor. If you want another view of your bloodsugar data: Code it up, and have a live-view using this app.

### Progress:
- [x] Read data
- [x] View of last 8 and 24 hours, last week and month
- [x] View of entire history
- [x] Saving raw data to database
- [x] Enable export of database outside of phone
- [x] Log manual readings from glucose reader to improve calibration.
- [x] Handle multiple sensors.
- [x] Icons based on if glucose level is increasing or decreasing.
- [ ] Calibration of sensor data based on glucose readings from the same time.
- [ ] More plots of glucose data.
- [ ] Log insulin injections
- [ ] Customize settings (units, target interval, etc)
- [ ] Design a logo
- [ ] Upload app to Google Play
- [ ] Set alarms


### Warning:
- If you actually end up using this app, it's your responsibility to take care of your diabetes. If the app does not work propely I take no responsibility, yadayada. If something is not working as intended, post an issue, or a pull request.

- Use only when the sensor has been activated. I've not yet looked into what data is sent when the sensor starts, trying to poll data during the startup period might not be good for the sensor.


### Todo in readme:
- Installation / setup of env in Android Studio
- Describe how it works, (me patternmatching)
- Rant some more/Credit
- Screenshots
