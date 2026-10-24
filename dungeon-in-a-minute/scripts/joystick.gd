extends Control
## Floating joystick: appears wherever the thumb lands and follows it if it
## drags past the edge. `vector` is the movement input, length 0..1.

@export var radius := 110.0
@export var deadzone := 0.12

var vector := Vector2.ZERO

var _touch_index := -1
var _origin := Vector2.ZERO
var _knob := Vector2.ZERO


func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed and _touch_index == -1:
			_touch_index = event.index
			_origin = event.position
			_move_knob(event.position)
		elif not event.pressed and event.index == _touch_index:
			release()
	elif event is InputEventScreenDrag and event.index == _touch_index:
		_move_knob(event.position)


func release() -> void:
	_touch_index = -1
	vector = Vector2.ZERO
	queue_redraw()


func _move_knob(pos: Vector2) -> void:
	var offset := pos - _origin
	if offset.length() > radius:
		# Drag the base along so the stick never "runs out".
		_origin = pos - offset.normalized() * radius
		offset = pos - _origin
	_knob = pos
	var v := offset / radius
	vector = v if v.length() > deadzone else Vector2.ZERO
	queue_redraw()


func _draw() -> void:
	if _touch_index == -1:
		return
	draw_circle(_origin, radius, Color(1, 1, 1, 0.08))
	draw_arc(_origin, radius, 0.0, TAU, 48, Color(1, 1, 1, 0.35), 3.0, true)
	draw_circle(_knob, radius * 0.4, Color(1, 1, 1, 0.45))
