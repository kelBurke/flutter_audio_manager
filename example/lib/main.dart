import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter_audio_manager/flutter_audio_manager.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  AudioInput _currentInput = AudioInput("unknow", 0);
  FlutterAudioManager _flutterAudioManager = FlutterAudioManager();
  List<AudioInput> _availableInputs = [];

  @override
  void initState() {
    super.initState();
    init();
  }

  Future<void> init() async {
    _flutterAudioManager.setListener(() async {
      print("-----changed-------");
      await _getInput();
      setState(() {});
    });

    await _getInput();
    if (!mounted) return;
    setState(() {});
  }

  _getInput() async {
    _currentInput = await _flutterAudioManager.getCurrentOutput();
    print("current:$_currentInput");
    _availableInputs = await _flutterAudioManager.getAvailableInputs();
    print("available $_availableInputs");
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(10),
            child: Column(
              children: <Widget>[
                Text(
                  "current output:${_currentInput.name} ${_currentInput.port}",
                ),
                Divider(),
                Expanded(
                  child: ListView.builder(
                    itemBuilder: (_, index) {
                      AudioInput input = _availableInputs[index];
                      return Row(
                        children: <Widget>[
                          Expanded(child: Text("${input.name}")),
                          Expanded(child: Text("${input.port}")),
                        ],
                      );
                    },
                    itemCount: _availableInputs.length,
                  ),
                ),
              ],
            ),
          ),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () async {
            final currentInput = await _flutterAudioManager.getCurrentOutput();
            print('Current Input');
            print(currentInput.name);
            print(currentInput.port.index);
            final avaliableOutputs =
                await _flutterAudioManager.getAvailableInputs();
            avaliableOutputs.forEach((element) {
              print('Avaliable');
              print(element.name);
            });
            
            // bool res = false;
            // if (_currentInput.port == AudioPort.receiver) {
            //   res = await _flutterAudioManager.changeToSpeaker();
            //   print("change to speaker:$res");
            // } else {
            //   res = await _flutterAudioManager.changeToReceiver();
            //   print("change to receiver:$res");
            // }
            // await _getInput();
          },
        ),
      ),
    );
  }
}
