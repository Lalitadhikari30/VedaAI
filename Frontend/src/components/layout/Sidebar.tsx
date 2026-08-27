import { 
  LayoutGrid, Monitor, ClipboardList, FileText, Library, Settings, 
  Sparkles, ChevronsRight, PanelLeftClose, Clock
} from 'lucide-react';
import clsx from 'clsx';

interface SidebarProps {
  collapsed?: boolean;
  onToggle?: () => void;
}

const navItems = [
  { icon: LayoutGrid, label: 'Home', active: false },
  { icon: Monitor, label: 'My Classroom', active: false },
  { icon: ClipboardList, label: 'Assignments', active: false },
  { icon: FileText, label: 'Exams', active: true },
  { icon: Library, label: 'My Library', active: false },
  { icon: Clock, label: 'History', active: false },
];

export default function Sidebar({ collapsed = true, onToggle }: SidebarProps) {
  if (collapsed) {
    return (
      <aside className="hidden lg:flex flex-col items-center w-[68px] bg-[#16171D] border-r border-[#262832] h-screen sticky top-0 z-30 py-5 flex-shrink-0 select-none justify-between transition-all duration-200">
        {/* Top Section */}
        <div className="flex flex-col items-center w-full gap-5">
          {/* Top Logo Mark */}
          <div 
            onClick={onToggle}
            title="Expand Sidebar"
            className="w-10 h-10 bg-gradient-to-b from-[#2C2E3B] to-[#1C1D26] rounded-2xl flex items-center justify-center border border-white/10 shadow-md flex-shrink-0 cursor-pointer hover:border-white/20 transition-colors"
          >
            <span className="text-white font-black text-xl leading-none tracking-tighter select-none">V</span>
          </div>

          {/* AI Teacher's Toolkit Icon Button with Orange Glow */}
          <div className="w-full flex justify-center">
            <button 
              title="AI Teacher's Toolkit"
              className="w-11 h-11 rounded-full bg-gradient-to-tr from-[#D94820] to-[#F27448] text-white flex items-center justify-center shadow-[0_0_16px_rgba(232,98,60,0.45)] ring-2 ring-[#F27448]/30 transition-transform hover:scale-105 active:scale-95 cursor-pointer"
            >
              <Sparkles size={18} className="text-white" />
            </button>
          </div>

          {/* Icon Navigation Rail */}
          <nav className="flex flex-col items-center gap-2.5 w-full px-2 mt-1">
            {navItems.map((item) => (
              <button
                key={item.label}
                title={item.label}
                className={clsx(
                  'w-11 h-11 rounded-xl flex items-center justify-center transition-all cursor-pointer group relative',
                  item.active
                    ? 'bg-white/10 text-white shadow-xs'
                    : 'text-[#7E8299] hover:bg-white/5 hover:text-white'
                )}
              >
                <item.icon 
                  size={20} 
                  className={clsx(
                    'transition-colors',
                    item.active ? 'text-white' : 'text-[#7E8299] group-hover:text-white'
                  )} 
                />
              </button>
            ))}
          </nav>
        </div>

        {/* Bottom Section: School Badge & Collapse Controls */}
        <div className="flex flex-col items-center gap-3 w-full pb-1">
          {/* School Crest Badge */}
          <div 
            title="Universal Public School, New Delhi"
            className="w-10 h-10 rounded-full bg-white/10 border border-white/15 p-1 flex items-center justify-center shadow-xs cursor-pointer hover:bg-white/20 transition-colors"
          >
            <img 
              src="/school-crest.svg" 
              alt="Universal Public School, New Delhi" 
              className="w-full h-full object-contain"
            />
          </div>

          {/* Settings Icon */}
          <button
            title="Settings"
            className="w-10 h-10 rounded-xl flex items-center justify-center text-[#7E8299] hover:bg-white/5 hover:text-white transition-colors cursor-pointer"
          >
            <Settings size={18} />
          </button>

          {/* Expand / Toggle Arrow Button */}
          <button
            onClick={onToggle}
            title="Expand Sidebar"
            className="w-8 h-8 rounded-lg flex items-center justify-center text-[#5E6278] hover:bg-white/10 hover:text-white transition-colors cursor-pointer"
          >
            <ChevronsRight size={17} />
          </button>
        </div>
      </aside>
    );
  }

  // Expanded Sidebar State
  return (
    <aside className="hidden lg:flex flex-col w-[260px] bg-[#16171D] border-r border-[#262832] h-screen sticky top-0 z-30 flex-shrink-0 select-none justify-between transition-all duration-200">
      {/* Top Section */}
      <div className="flex flex-col w-full">
        {/* Top Logo Row with Close Button */}
        <div className="flex items-center justify-between px-5 pt-5 pb-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-b from-[#2C2E3B] to-[#1C1D26] rounded-2xl flex items-center justify-center border border-white/10 shadow-md flex-shrink-0">
              <span className="text-white font-black text-xl leading-none tracking-tighter">V</span>
            </div>
            <span className="text-white font-bold text-[19px] tracking-tight">VedaAI</span>
          </div>
          
          <button
            onClick={onToggle}
            title="Collapse Sidebar"
            className="w-8 h-8 rounded-lg flex items-center justify-center text-[#7E8299] hover:bg-white/10 hover:text-white transition-colors cursor-pointer"
          >
            <PanelLeftClose size={18} />
          </button>
        </div>

        {/* AI Teacher's Toolkit Full Button */}
        <div className="px-4 mt-3 mb-5">
          <button className="w-full py-2.5 px-4 rounded-full bg-gradient-to-tr from-[#D94820] to-[#F27448] text-white font-semibold text-[13px] flex items-center justify-center gap-2 shadow-[0_0_16px_rgba(232,98,60,0.35)] hover:brightness-110 active:scale-98 transition-all cursor-pointer">
            <Sparkles size={16} className="text-white flex-shrink-0" />
            <span>AI Teacher's Toolkit</span>
          </button>
        </div>

        {/* Navigation List with Labels */}
        <nav className="px-3 flex flex-col gap-1.5">
          {navItems.map((item) => (
            <button
              key={item.label}
              className={clsx(
                'w-full flex items-center gap-3.5 px-3.5 py-2.5 rounded-xl text-[14px] font-medium transition-all text-left cursor-pointer group',
                item.active
                  ? 'bg-white/10 text-white font-semibold shadow-xs'
                  : 'text-[#7E8299] hover:bg-white/5 hover:text-white'
              )}
            >
              <item.icon 
                size={19} 
                className={clsx(
                  'transition-colors',
                  item.active ? 'text-white' : 'text-[#7E8299] group-hover:text-white'
                )} 
              />
              <span className="tracking-tight">{item.label}</span>
            </button>
          ))}
        </nav>
      </div>

      {/* Bottom Section */}
      <div className="flex flex-col w-full px-3 pb-4">
        {/* Settings Item */}
        <button className="w-full flex items-center gap-3.5 px-3.5 py-2.5 rounded-xl text-[14px] font-medium text-[#7E8299] hover:bg-white/5 hover:text-white transition-all text-left cursor-pointer mb-3">
          <Settings size={19} />
          <span className="tracking-tight">Settings</span>
        </button>

        {/* School Card: Universal Public School, New Delhi */}
        <div className="p-3 bg-white/5 rounded-2xl border border-white/10 flex items-center gap-3 shadow-xs">
          <div className="w-9 h-9 rounded-full bg-white/10 flex items-center justify-center border border-white/10 p-1 flex-shrink-0">
            <img 
              src="/school-crest.svg" 
              alt="Universal Public School" 
              className="w-full h-full object-contain"
            />
          </div>
          <div className="min-w-0">
            <div className="text-[13px] font-bold text-white truncate leading-snug">Universal Public School</div>
            <div className="text-[11px] text-[#7E8299] font-medium truncate">New Delhi</div>
          </div>
        </div>
      </div>
    </aside>
  );
}
