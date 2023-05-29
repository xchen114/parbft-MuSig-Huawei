for /L %%i in (0,1,13) do start "server" cmd /k call smartrun.bat parbft.demo.counter.CounterServer %%i

exit