import model.*;

import java.util.*;

public final class MyStrategy implements Strategy {
    private static final double WAYPOINT_RADIUS = 100.0D;
    private static final double LOW_HP_FACTOR = 0.70D;
    private static boolean TURNING = false;

    /**
     TODO
     1. Нужно научиться объходить других юнитов, если уперся в них
     2. добавить waypoints
     * */

    /**
     * Ключевые точки для каждой линии, позволяющие упростить управление перемещением волшебника.
     * <p>
     * Если всё хорошо, двигаемся к следующей точке и атакуем противников.
     * Если осталось мало жизненной энергии, отступаем к предыдущей точке.
     */
    private final Map<LaneType, Point2D[]> waypointsByLane = new EnumMap<>(LaneType.class);

    private Random random;

    private LaneType lane;
    private Point2D[] waypoints;

    private Wizard self;
    private World world;
    private Game game;
    private Move move;


    /**
     * Основной метод стратегии, осуществляющий управление волшебником.
     * Вызывается каждый тик для каждого волшебника.
     *
     * @param self  Волшебник, которым данный метод будет осуществлять управление.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        initializeStrategy(self, game);
        initializeTick(self, world, game, move);
        if (!TURNING) {
            evasionManeuver();
        }
        if (game.isSkillsEnabled()) {
            learning();
        }
        if (getVisibleEnemies().size() > 0) {
            battleMoving();
        } else {
            nonBattleMoving();
        }
    }

    /**
     * Изучаем скиллы
     */
    private void learning() {
        if (!Arrays.asList(self.getSkills()).contains(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1)) {
            move.setSkillToLearn(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1);
        }
        if (!Arrays.asList(self.getSkills()).contains(SkillType.STAFF_DAMAGE_BONUS_AURA_1)) {
            move.setSkillToLearn(SkillType.STAFF_DAMAGE_BONUS_AURA_1);
        }
        if (!Arrays.asList(self.getSkills()).contains(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2)) {
            move.setSkillToLearn(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2);
        }
        if (!Arrays.asList(self.getSkills()).contains(SkillType.STAFF_DAMAGE_BONUS_AURA_2)) {
            move.setSkillToLearn(SkillType.STAFF_DAMAGE_BONUS_AURA_2);
        }
        if (!Arrays.asList(self.getSkills()).contains(SkillType.FIREBALL)) {
            move.setSkillToLearn(SkillType.FIREBALL);
        }
    }

    private void nonBattleMoving() {
        goTo(getNextWaypoint());
    }

    private void battleMoving() {
        if (self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) {
            evasionManeuver();
            goTo(getPreviousWaypoint());

        } else {
            if (!canBeAttackedMoreThanOneEnemy()) {
                //for one target
                if (getTargets().size() == 0) {
                    if (getVisibleEnemies().size() == 0) {
                        goTo(getNextWaypoint());
                    } else {
                        goTo(selectTarget(getVisibleEnemies(), "NEAREST"));
                    }
                } else {
                    attackTarget(selectTarget(getTargets(), "NEAREST"), true);
                }
            } else {
                //for several target
                recedeAndAttack();
            }
        }
    }

    private void evasionManeuver() {

        // Постоянно двигаемся из-стороны в сторону, чтобы по нам было сложнее попасть.
        // Считаете, что сможете придумать более эффективный алгоритм уклонения? Попробуйте! ;)
        int evasionSign = (random.nextBoolean() ? 1 : -1);
        int evasionActionsCount = evasionSign * random.nextInt(9);
        for (int i = 0; i < Math.abs(evasionActionsCount); i++) {
            move.setStrafeSpeed(evasionSign * game.getWizardStrafeSpeed());
        }
        /*
        move.setStrafeSpeed(random.nextBoolean() ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed());
        move.setStrafeSpeed(random.nextBoolean() ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed());
        move.setStrafeSpeed(random.nextBoolean() ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed());*/
    }

    private void setTurnAdvanced(double angle) {
        TURNING = true;
        move.setTurn(angle);
    }

    /**
     * Инциализируем стратегию.
     * <p>
     * Для этих целей обычно можно использовать конструктор, однако в данном случае мы хотим инициализировать генератор
     * случайных чисел значением, полученным от симулятора игры.
     */
    private void initializeStrategy(Wizard self, Game game) {
        if (random == null) {
            random = new Random(game.getRandomSeed());

            double mapSize = game.getMapSize();

            waypointsByLane.put(LaneType.MIDDLE, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    random.nextBoolean()
                            ? new Point2D(600.0D, mapSize - 200.0D)
                            : new Point2D(200.0D, mapSize - 600.0D),
                    new Point2D(800.0D, mapSize - 800.0D),
                    new Point2D(mapSize - 600.0D, 600.0D)
            });


            waypointsByLane.put(LaneType.TOP, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(100.0D, mapSize - 400.0D),
                    new Point2D(200.0D, mapSize - 800.0D),
                    new Point2D(200.0D, mapSize * 0.75D),
                    new Point2D(200.0D, mapSize * 0.5D),
                    new Point2D(200.0D, mapSize * 0.25D),
                    new Point2D(200.0D, 200.0D),
                    new Point2D(mapSize * 0.25D, 200.0D),
                    new Point2D(mapSize * 0.5D, 200.0D),
                    new Point2D(mapSize * 0.75D, 200.0D),
                    new Point2D(mapSize - 200.0D, 200.0D)
            });

            waypointsByLane.put(LaneType.BOTTOM, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(400.0D, mapSize - 100.0D),
                    new Point2D(800.0D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.25D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.5D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.75D, mapSize - 200.0D),
                    new Point2D(mapSize - 200.0D, mapSize - 200.0D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.75D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.5D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.25D),
                    new Point2D(mapSize - 200.0D, 200.0D)
            });

            switch ((int) self.getId()) {
                case 1:
                case 2:
                case 6:
                case 7:
                    lane = LaneType.TOP;
                    break;
                case 3:
                case 8:
                    lane = LaneType.MIDDLE;
                    break;
                case 4:
                case 5:
                case 9:
                case 10:
                    lane = LaneType.BOTTOM;
                    break;
                default:
            }

            waypoints = waypointsByLane.get(lane);

            // Наша стратегия исходит из предположения, что заданные нами ключевые точки упорядочены по убыванию
            // дальности до последней ключевой точки. Сейчас проверка этого факта отключена, однако вы можете
            // написать свою проверку, если решите изменить координаты ключевых точек.

            /*Point2D lastWaypoint = waypoints[waypoints.length - 1];

            Preconditions.checkState(ArrayUtils.isSorted(waypoints, (waypointA, waypointB) -> Double.compare(
                    waypointB.getDistanceTo(lastWaypoint), waypointA.getDistanceTo(lastWaypoint)
            )));*/
        }
    }

    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    private void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
    }

    /**
     * Данный метод предполагает, что все ключевые точки на линии упорядочены по уменьшению дистанции до последней
     * ключевой точки. Перебирая их по порядку, находим первую попавшуюся точку, которая находится ближе к последней
     * точке на линии, чем волшебник. Это и будет следующей ключевой точкой.
     * <p>
     * Дополнительно проверяем, не находится ли волшебник достаточно близко к какой-либо из ключевых точек. Если это
     * так, то мы сразу возвращаем следующую ключевую точку.
     */
    private Point2D getNextWaypoint() {
        int lastWaypointIndex = waypoints.length - 1;
        Point2D lastWaypoint = waypoints[lastWaypointIndex];

        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex + 1];
            }

            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return lastWaypoint;
    }

    /**
     * Действие данного метода абсолютно идентично действию метода {@code getNextWaypoint}, если перевернуть массив
     * {@code waypoints}.
     */
    private Point2D getPreviousWaypoint() {
        Point2D firstWaypoint = waypoints[0];

        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex - 1];
            }

            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return firstWaypoint;
    }

    /**
     * Простейший способ перемещения волшебника.
     */
    private void goTo(Point2D point) {
        double angle = self.getAngleTo(point.getX(), point.getY());
        if (StrictMath.abs(angle) > StrictMath.PI / 24) {
            double backAngle = StrictMath.abs(angle) > StrictMath.PI / 2 ?
                    -(angle + StrictMath.PI - (angle > 0 ? 2 * StrictMath.PI : 0)) : angle;
            double forwardWalkingTime = angle / game.getWizardMaxTurnAngle()
                    + self.getDistanceTo(point.getX(), point.getY()) / game.getWizardForwardSpeed();
            double strafeWalkingTime = (StrictMath.abs(angle) - StrictMath.PI / 2) / game.getWizardMaxTurnAngle()
                    + self.getDistanceTo(point.getX(), point.getY()) / game.getWizardStrafeSpeed();
            double backwardWalkingTime = backAngle / game.getWizardMaxTurnAngle()
                    + self.getDistanceTo(point.getX(), point.getY()) / game.getWizardBackwardSpeed();
            double minTime = StrictMath.min(StrictMath.min(forwardWalkingTime, strafeWalkingTime), backwardWalkingTime);
            if (minTime == forwardWalkingTime) {
                setTurnAdvanced(angle);
                move.setSpeed(game.getWizardForwardSpeed());
            } else if (minTime == strafeWalkingTime) {
                if (angle > 0) {
                    setTurnAdvanced(angle + StrictMath.PI / 2);
                    move.setStrafeSpeed(game.getWizardStrafeSpeed());
                } else {
                    setTurnAdvanced(angle - StrictMath.PI / 2);
                    move.setStrafeSpeed(-game.getWizardStrafeSpeed());
                }
            } else if (minTime == backwardWalkingTime) {
                setTurnAdvanced(backAngle);
                move.setSpeed(game.getWizardBackwardSpeed());
            }
        } else {
            setTurnAdvanced(angle);
            if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
                move.setSpeed(game.getWizardForwardSpeed());
            }
        }
    }

    /**
     * Находим ближайшую цель для атаки, независимо от её типа и других характеристик.
     */
    private LivingUnit getNearestTarget() {
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getWizards()));
        targets.addAll(Arrays.asList(world.getMinions()));
        List<Integer> targetsLife = new ArrayList<>();
//        double minHealth = GAME_MAX_LIFE_POINT;
//        for (LivingUnit target : targets) {
//            double currentLifePoint = target.getLife();
//            minHealth = Math.min(currentLifePoint, minHealth);
//            targetsLife.add(target.getLife());
//
//        }
        LivingUnit nearestTarget = null;
        double nearestTargetDistance = Double.MAX_VALUE;

        for (LivingUnit target : targets) {
            if (target.getFaction() == Faction.NEUTRAL || target.getFaction() == self.getFaction()) {
                continue;
            }
            double distance = self.getDistanceTo(target);
            if (distance < nearestTargetDistance) {
                nearestTarget = target;
                nearestTargetDistance = distance;
            }
          /*  if (target.getLife() == minHealth) {
                nearestTarget = target;
            }*/
        }

        return nearestTarget;
    }

    /**
     * Вспомогательный класс для хранения позиций на карте.
     */
    private static final class Point2D {
        private final double x;
        private final double y;

        private Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        /*public*/ double getDistanceTo(double x, double y) {
            return StrictMath.hypot(this.x - x, this.y - y);
        }

        /*public*/ double getDistanceTo(Point2D point) {
            return getDistanceTo(point.x, point.y);
        }

        /*public*/ double getDistanceTo(Unit unit) {
            return getDistanceTo(unit.getX(), unit.getY());
        }
    }

    /**
     * получение списка видимых врагов
     */
    private ArrayList<LivingUnit> getVisibleEnemies() {
        ArrayList<LivingUnit> enemies = new ArrayList<>();
        for (Minion minion : world.getMinions()) {
            if (minion.getFaction() != Faction.NEUTRAL && minion.getFaction() != self.getFaction()) {
                enemies.add(minion);
            }
        }
        for (Wizard wizard : world.getWizards()) {
            if (wizard.getFaction() != self.getFaction()) {
                enemies.add(wizard);
            }
        }
        for (Building build : world.getBuildings()) {
            if (build.getFaction() != self.getFaction()) {
                enemies.add(build);
            }
        }
        return enemies;
    }

    /**
     * получение списка целей, находящихся в радиусе атаки
     */
    private ArrayList<LivingUnit> getTargets() {
        ArrayList<LivingUnit> targets = new ArrayList<>();
        for (LivingUnit enemy : getVisibleEnemies()) {
            if (self.getDistanceTo(enemy) <= self.getCastRange() || self.getDistanceTo(enemy) <= game.getStaffRange()) {
                targets.add(enemy);
            }
        }
        return targets;
    }

    /**
     * Проверяем, можем ли мы быть атакованы более чем одним противником
     */
    private boolean canBeAttackedMoreThanOneEnemy() {
        double fetishAttackRange = 300; // TODO need to clarify
        double orcAttackRange = 50;// TODO need to clarify
        List<LivingUnit> potentialAbuser = new ArrayList<>();
        for (LivingUnit livingUnit : getVisibleEnemies()) {
            if (livingUnit instanceof Minion) {
                if (((Minion) livingUnit).getType() == MinionType.ORC_WOODCUTTER && (self.getDistanceTo(livingUnit) + self.getRadius()) <= orcAttackRange) {
                    potentialAbuser.add(livingUnit);
                }
                if (((Minion) livingUnit).getType() == MinionType.FETISH_BLOWDART && (self.getDistanceTo(livingUnit) + self.getRadius()) <= fetishAttackRange) {
                    potentialAbuser.add(livingUnit);
                }
            }
            if (livingUnit instanceof Wizard) {
                if (((Wizard) livingUnit).getCastRange() >= self.getDistanceTo(livingUnit) + self.getRadius() || game.getStaffRange() >= self.getDistanceTo(livingUnit) + self.getRadius()) {
                    potentialAbuser.add(livingUnit);
                }
            }
            if (livingUnit instanceof Building) {
                if (((Building) livingUnit).getAttackRange() >= self.getDistanceTo(livingUnit)) {
                    potentialAbuser.add(livingUnit);
                }
            }
        }
        return potentialAbuser.size() > 1;
    }

    private LivingUnit selectTarget(List<LivingUnit> possibleTargets, String selectingFactor) {
        LivingUnit selectedTarget = null;
        // пока будем брать ближайшую цель
        double minDistance = Double.MAX_VALUE;
        for (LivingUnit possibleTarget : possibleTargets) {
            if (minDistance > self.getDistanceTo(possibleTarget)) {
                minDistance = self.getDistanceTo(possibleTarget);
                selectedTarget = possibleTarget;
            }
        }
        return selectedTarget;
    }

    private void goTo(LivingUnit livingUnit) {
        move.setTurn(self.getAngleTo(livingUnit)); // todo add game.staffAngle
        move.setSpeed(game.getWizardForwardSpeed());
    }

    private void attackTarget(LivingUnit livingUnit, Boolean strict) {
/**
 * strict = true - обязательно повернуться к цели
 = false - не поворачиваться к цели
 */
        double angle = self.getAngleTo(livingUnit);
        if (strict) {
            move.setTurn(angle);
        }
        double distance = self.getDistanceTo(livingUnit);
        if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
            // ... то атакуем.
            move.setAction(ActionType.MAGIC_MISSILE);
            move.setCastAngle(angle);
            move.setMinCastDistance(distance - livingUnit.getRadius() + game.getMagicMissileRadius());
        }

    }

    private void recedeAndAttack() {
        Point2D recedePoint = getPreviousWaypoint();
        double angle = self.getAngleTo(recedePoint.getX(), recedePoint.getY());
        move.setTurn(angle > 0 ? angle - StrictMath.PI : angle + StrictMath.PI);
        move.setSpeed(-game.getWizardBackwardSpeed());
        attackTarget(selectTarget(getTargets(), "LOW_HP"), false);
    }
}